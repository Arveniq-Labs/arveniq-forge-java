package io.arveniq.forge;

import java.util.Map;

/** Current API-key rate-limit state. */
public record ForgeRateLimitState(
    String organizationId, String workspaceId, String apiKeyId, long windowSeconds,
    long requestLimit, Long burstLimit, long currentUsage, long remaining, String resetAt, boolean exceeded) {
  public static ForgeRateLimitState from(Map<String, Object> value) {
    return new ForgeRateLimitState(Json.string(value, "organizationId"), Json.string(value, "workspaceId"), Json.string(value, "apiKeyId"),
        number(value.get("windowSeconds")), number(value.get("requestLimit")), nullableNumber(value.get("burstLimit")),
        number(value.get("currentUsage")), number(value.get("remaining")), Json.string(value, "resetAt"), Json.bool(value, "exceeded", false));
  }
  private static long number(Object value) { return value instanceof Number number ? number.longValue() : 0L; }
  private static Long nullableNumber(Object value) { return value instanceof Number number ? number.longValue() : null; }
}
