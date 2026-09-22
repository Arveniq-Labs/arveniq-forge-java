package io.arveniq.forge.widget;

import io.arveniq.forge.Json;
import java.util.Map;

/** Parsed, untrusted browser payload produced by {@link ForgeChatWidget}. */
public record ForgeWidgetRequest(String message, String clientMessageId, Map<String, Object> browserContext) {
  public static ForgeWidgetRequest fromJson(String payload) {
    Map<String, Object> value = Json.object(payload);
    String message = Json.string(value, "message");
    String clientMessageId = Json.string(value, "clientMessageId");
    if (message == null || message.isBlank()) throw new IllegalArgumentException("message is required.");
    if (clientMessageId == null || clientMessageId.isBlank()) throw new IllegalArgumentException("clientMessageId is required.");
    return new ForgeWidgetRequest(message, clientMessageId, Json.map(value.get("context")));
  }
}
