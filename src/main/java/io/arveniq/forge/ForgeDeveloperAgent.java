package io.arveniq.forge;

import java.util.Map;

/** A Forge agent visible to the scoped developer API key. */
public record ForgeDeveloperAgent(
    String id, String name, String slug, String description, String category, String status,
    String riskLevel, Map<String, Object> project, Map<String, Object> latestVersion,
    String createdAt, String updatedAt) {
  public static ForgeDeveloperAgent from(Map<String, Object> value) {
    return new ForgeDeveloperAgent(
        Json.string(value, "id"), Json.string(value, "name"), Json.string(value, "slug"),
        Json.string(value, "description"), Json.string(value, "category"), Json.string(value, "status"),
        Json.string(value, "riskLevel"), Json.map(value.get("project")), Json.map(value.get("latestVersion")),
        Json.string(value, "createdAt"), Json.string(value, "updatedAt"));
  }
}
