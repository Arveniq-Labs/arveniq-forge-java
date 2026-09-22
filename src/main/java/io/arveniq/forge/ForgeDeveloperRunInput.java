package io.arveniq.forge;

import java.util.LinkedHashMap;
import java.util.Map;

/** Input for a developer run. Context must be resolved and authorized by the calling server. */
public record ForgeDeveloperRunInput(String idempotencyKey, Object input) implements Json.WritableJson {
  public static ForgeDeveloperRunInput of(Object input) { return new ForgeDeveloperRunInput(null, input); }
  public static ForgeDeveloperRunInput of(String idempotencyKey, Object input) { return new ForgeDeveloperRunInput(idempotencyKey, input); }
  @Override public Map<String, Object> toJson() {
    Map<String, Object> result = new LinkedHashMap<>();
    if (idempotencyKey != null && !idempotencyKey.isBlank()) result.put("idempotencyKey", idempotencyKey);
    if (input != null) result.put("input", input);
    return result;
  }
}
