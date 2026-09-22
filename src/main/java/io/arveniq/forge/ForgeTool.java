package io.arveniq.forge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal dependency-free CLI for governed tool-package validation and deployment. */
public final class ForgeTool {
  private ForgeTool() {}
  public static void main(String[] args) {
    try { run(args); } catch (Exception exception) { System.err.println(exception.getMessage()); System.exit(1); }
  }
  static void run(String[] args) throws Exception {
    String command = args.length == 0 ? "help" : args[0]; Map<String, String> flags = flags(args);
    if ("help".equals(command) || "--help".equals(command) || "-h".equals(command)) { help(); return; }
    if ("validate".equals(command)) {
      ValidationResult validation = ForgeManifests.validateJson(readManifest(flags.getOrDefault("manifest", "tool-package.json")));
      System.out.println(Json.stringify(validationJson(validation)));
      if (!validation.valid()) System.exit(1); return;
    }
    ForgeToolPackagesClient client = new ForgeToolPackagesClient(ForgeClientOptions.builder()
        .baseUrl(flags.getOrDefault("baseUrl", environment("FORGE_API_URL", "http://localhost:4000/v1")))
        .apiKey(required(flags.getOrDefault("apiKey", environment("FORGE_API_KEY", null)), "--api-key or FORGE_API_KEY")).build());
    switch (command) {
      case "submit" -> print(client.submitPackage(required(flags.get("packageId"), "--package-id")));
      case "create-version" -> {
        Map<String, Object> manifest = readManifest(flags.getOrDefault("manifest", "tool-package.json"));
        Map<String, Object> input = new LinkedHashMap<>(); input.put("version", flags.getOrDefault("version", Json.string(manifest, "version")));
        input.put("commitSha", flags.get("commitSha")); input.put("artifactRef", flags.get("artifactRef"));
        input.put("environment", environmentEnum(flags.getOrDefault("environment", "DEVELOPMENT"))); input.put("manifest", manifest);
        print(client.createVersion(required(flags.get("packageId"), "--package-id"), input));
      }
      case "validate-version" -> print(client.validateVersion(required(flags.get("versionId"), "--version-id")));
      case "deploy-version" -> print(client.deployVersion(required(flags.get("versionId"), "--version-id"), environmentEnum(required(flags.get("environment"), "--environment")), flags.get("reason")));
      default -> throw new IllegalArgumentException("Unknown command: " + command);
    }
  }
  private static Map<String, Object> readManifest(String file) throws Exception { return Json.object(Files.readString(Path.of(file))); }
  private static Map<String, String> flags(String[] args) {
    Map<String, String> values = new LinkedHashMap<>();
    for (int index = 1; index < args.length; index++) if (args[index].startsWith("--")) {
      String key = dashedToCamel(args[index].substring(2));
      values.put(key, index + 1 < args.length && !args[index + 1].startsWith("--") ? args[++index] : "true");
    }
    return values;
  }
  private static String required(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required."); return value; }
  private static String environment(String key, String fallback) { String value = System.getenv(key); return value == null || value.isBlank() ? fallback : value; }
  private static DeploymentEnvironment environmentEnum(String value) { try { return DeploymentEnvironment.valueOf(value); } catch (Exception ignored) { throw new IllegalArgumentException("Unsupported environment: " + value); } }
  private static void print(Map<String, Object> value) { System.out.println(Json.stringify(value)); }
  private static String dashedToCamel(String value) {
    StringBuilder result = new StringBuilder(); boolean capitalize = false;
    for (char character : value.toCharArray()) { if (character == '-') { capitalize = true; continue; } result.append(capitalize ? Character.toUpperCase(character) : character); capitalize = false; }
    return result.toString();
  }
  private static Map<String, Object> validationJson(ValidationResult validation) {
    return Map.of("valid", validation.valid(), "errors", validation.errors().stream().map(issue -> Map.of("path", issue.path(), "message", issue.message(), "severity", issue.severity().toString().toLowerCase())).toList(), "warnings", validation.warnings().stream().map(issue -> Map.of("path", issue.path(), "message", issue.message(), "severity", issue.severity().toString().toLowerCase())).toList());
  }
  private static void help() { System.out.println("""
      Arveniq Forge Java CLI

      Usage:
        forge-tool validate --manifest tool-package.json
        forge-tool submit --package-id pkg_123
        forge-tool create-version --package-id pkg_123 --manifest tool-package.json --version 1.0.0 --commit-sha abc123
        forge-tool validate-version --version-id version_123
        forge-tool deploy-version --version-id version_123 --environment STAGING --reason "Release candidate"
      """); }
}
