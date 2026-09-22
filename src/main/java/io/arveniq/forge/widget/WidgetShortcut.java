package io.arveniq.forge.widget;

import java.util.Locale;
import java.util.Map;

/** A keyboard shortcut that opens a {@link ForgeChatWidget}. */
public record WidgetShortcut(boolean meta, boolean ctrl, boolean alt, boolean shift, String key) {
  public WidgetShortcut {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("Shortcut key is required.");
    key = key.trim().toUpperCase(Locale.ROOT);
  }
  public static WidgetShortcut meta(String key) { return new WidgetShortcut(true, false, false, false, key); }
  public static WidgetShortcut control(String key) { return new WidgetShortcut(false, true, false, false, key); }
  /** Parses strings such as {@code META+K}, {@code CTRL+SHIFT+SPACE}, and {@code ALT+J}. */
  public static WidgetShortcut parse(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Shortcut is required.");
    boolean meta = false, ctrl = false, alt = false, shift = false; String key = null;
    for (String part : value.trim().toUpperCase(Locale.ROOT).split("\\+")) switch (part) {
      case "META", "CMD", "COMMAND" -> meta = true;
      case "CTRL", "CONTROL" -> ctrl = true;
      case "ALT", "OPTION" -> alt = true;
      case "SHIFT" -> shift = true;
      default -> { if (key != null) throw new IllegalArgumentException("Shortcut must contain one key."); key = part; }
    }
    return new WidgetShortcut(meta, ctrl, alt, shift, key);
  }
  public String display() {
    StringBuilder value = new StringBuilder();
    if (meta) value.append("⌘ "); if (ctrl) value.append("Ctrl+"); if (alt) value.append("⌥ "); if (shift) value.append("⇧ ");
    return value.append("SPACE".equals(key) ? "Space" : key).toString();
  }
  Map<String, Object> toJson() { return Map.of("meta", meta, "ctrl", ctrl, "alt", alt, "shift", shift, "key", key); }
}
