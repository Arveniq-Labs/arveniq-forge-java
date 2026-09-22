package io.arveniq.forge;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small dependency-free JSON codec used by the SDK's HTTP transport. */
public final class Json {
  private Json() {}

  public interface WritableJson {
    Map<String, Object> toJson();
  }

  public static String stringify(Object value) {
    StringBuilder output = new StringBuilder();
    write(output, value);
    return output.toString();
  }

  public static Object parse(String source) {
    Parser parser = new Parser(source);
    Object value = parser.value();
    parser.space();
    if (!parser.end()) throw new IllegalArgumentException("Unexpected JSON content after position " + parser.index + ".");
    return value;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> object(String source) {
    Object value = parse(source);
    if (!(value instanceof Map<?, ?>)) throw new IllegalArgumentException("Expected a JSON object.");
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> map(Object value) {
    if (!(value instanceof Map<?, ?>)) return Map.of();
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  public static List<Object> list(Object value) {
    if (!(value instanceof List<?>)) return List.of();
    return (List<Object>) value;
  }

  public static String string(Map<String, Object> value, String key) {
    Object item = value.get(key);
    return item == null ? null : String.valueOf(item);
  }

  public static boolean bool(Map<String, Object> value, String key, boolean fallback) {
    Object item = value.get(key);
    return item instanceof Boolean booleanValue ? booleanValue : fallback;
  }

  private static void write(StringBuilder output, Object value) {
    if (value == null) { output.append("null"); return; }
    if (value instanceof WritableJson serializable) { write(output, serializable.toJson()); return; }
    if (value instanceof String string) { quote(output, string); return; }
    if (value instanceof Character character) { quote(output, character.toString()); return; }
    if (value instanceof Boolean) { output.append(value); return; }
    if (value instanceof Number number) {
      if (number instanceof Double doubleValue && !Double.isFinite(doubleValue)
          || number instanceof Float floatValue && !Float.isFinite(floatValue)) {
        throw new IllegalArgumentException("JSON does not support non-finite numbers.");
      }
      output.append(number); return;
    }
    if (value instanceof Enum<?> enumeration) { quote(output, enumeration.toString()); return; }
    if (value instanceof Map<?, ?> map) {
      output.append('{'); boolean first = true;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!(entry.getKey() instanceof String key)) throw new IllegalArgumentException("JSON object keys must be strings.");
        if (!first) output.append(','); first = false;
        quote(output, key); output.append(':'); write(output, entry.getValue());
      }
      output.append('}'); return;
    }
    if (value instanceof Collection<?> values) {
      output.append('['); boolean first = true;
      for (Object item : values) { if (!first) output.append(','); first = false; write(output, item); }
      output.append(']'); return;
    }
    if (value.getClass().isArray()) {
      output.append('[');
      for (int index = 0; index < Array.getLength(value); index++) { if (index > 0) output.append(','); write(output, Array.get(value, index)); }
      output.append(']'); return;
    }
    throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
  }

  private static void quote(StringBuilder output, String value) {
    output.append('"');
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      switch (character) {
        case '"' -> output.append("\\\"");
        case '\\' -> output.append("\\\\");
        case '\b' -> output.append("\\b");
        case '\f' -> output.append("\\f");
        case '\n' -> output.append("\\n");
        case '\r' -> output.append("\\r");
        case '\t' -> output.append("\\t");
        case '<' -> output.append("\\u003c"); // Keeps JSON safe inside a script element.
        case '>' -> output.append("\\u003e");
        case '&' -> output.append("\\u0026");
        case '\u2028' -> output.append("\\u2028");
        case '\u2029' -> output.append("\\u2029");
        default -> {
          if (character < 0x20) output.append(String.format("\\u%04x", (int) character));
          else output.append(character);
        }
      }
    }
    output.append('"');
  }

  private static final class Parser {
    private final String source;
    private int index;
    private Parser(String source) { this.source = source == null ? "" : source; }
    private boolean end() { return index >= source.length(); }
    private void space() { while (!end() && Character.isWhitespace(source.charAt(index))) index++; }
    private Object value() {
      space(); if (end()) throw error("Expected JSON value");
      return switch (source.charAt(index)) {
        case '{' -> objectValue(); case '[' -> arrayValue(); case '"' -> stringValue();
        case 't' -> literal("true", Boolean.TRUE); case 'f' -> literal("false", Boolean.FALSE);
        case 'n' -> literal("null", null); default -> numberValue();
      };
    }
    private Object literal(String expected, Object value) {
      if (!source.startsWith(expected, index)) throw error("Expected " + expected);
      index += expected.length(); return value;
    }
    private Map<String, Object> objectValue() {
      index++; space(); Map<String, Object> result = new LinkedHashMap<>();
      if (consume('}')) return result;
      while (true) {
        space(); if (end() || source.charAt(index) != '"') throw error("Expected an object key");
        String key = stringValue(); space(); expect(':'); result.put(key, value()); space();
        if (consume('}')) return result; expect(',');
      }
    }
    private List<Object> arrayValue() {
      index++; space(); List<Object> result = new ArrayList<>(); if (consume(']')) return result;
      while (true) { result.add(value()); space(); if (consume(']')) return result; expect(','); }
    }
    private String stringValue() {
      expect('"'); StringBuilder result = new StringBuilder();
      while (!end()) {
        char character = source.charAt(index++);
        if (character == '"') return result.toString();
        if (character == '\\') {
          if (end()) throw error("Unterminated escape");
          char escaped = source.charAt(index++);
          switch (escaped) {
            case '"', '\\', '/' -> result.append(escaped); case 'b' -> result.append('\b');
            case 'f' -> result.append('\f'); case 'n' -> result.append('\n'); case 'r' -> result.append('\r');
            case 't' -> result.append('\t'); case 'u' -> result.append((char) hex());
            default -> throw error("Invalid string escape");
          }
        } else { if (character < 0x20) throw error("Control character in string"); result.append(character); }
      }
      throw error("Unterminated string");
    }
    private int hex() {
      if (index + 4 > source.length()) throw error("Invalid Unicode escape");
      String digits = source.substring(index, index + 4); index += 4;
      try { return Integer.parseInt(digits, 16); } catch (NumberFormatException exception) { throw error("Invalid Unicode escape"); }
    }
    private Number numberValue() {
      int start = index;
      if (consume('-')) { }
      if (consume('0')) { } else { digits(); }
      if (consume('.')) digits();
      if (consume('e') || consume('E')) { if (consume('+') || consume('-')) { } digits(); }
      if (start == index) throw error("Expected JSON value");
      try {
        BigDecimal decimal = new BigDecimal(source.substring(start, index));
        try { return decimal.longValueExact(); } catch (ArithmeticException ignored) { return decimal; }
      } catch (NumberFormatException exception) { throw error("Invalid number"); }
    }
    private void digits() { int start = index; while (!end() && Character.isDigit(source.charAt(index))) index++; if (start == index) throw error("Expected digit"); }
    private boolean consume(char expected) { if (!end() && source.charAt(index) == expected) { index++; return true; } return false; }
    private void expect(char expected) { space(); if (!consume(expected)) throw error("Expected '" + expected + "'"); }
    private IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at position " + index + "."); }
  }
}
