package io.arveniq.forge;

import java.util.Map;

/** A workflow visible to the scoped developer API key. */
public record ForgeDeveloperWorkflow(
    String id, String agentId, String agentName, String name, String slug, String status,
    Long latestVersion, String updatedAt) {
  public static ForgeDeveloperWorkflow from(Map<String, Object> value) {
    Object version = value.get("latestVersion");
    Long latest = version instanceof Number number ? number.longValue() : null;
    return new ForgeDeveloperWorkflow(Json.string(value, "id"), Json.string(value, "agentId"),
        Json.string(value, "agentName"), Json.string(value, "name"), Json.string(value, "slug"),
        Json.string(value, "status"), latest, Json.string(value, "updatedAt"));
  }
}
