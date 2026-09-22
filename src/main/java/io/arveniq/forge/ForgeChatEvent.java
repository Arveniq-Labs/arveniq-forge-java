package io.arveniq.forge;

import java.util.LinkedHashMap;
import java.util.Map;

/** A versioned event received from a Forge conversation SSE stream. */
public record ForgeChatEvent(
    String type, int version, String conversationId, String turnId, String messageId,
    String eventId, String sequence, String createdAt, Map<String, Object> data) implements Json.WritableJson {
  public static ForgeChatEvent from(Map<String, Object> value) {
    Object rawVersion = value.get("version");
    int version = rawVersion instanceof Number number ? number.intValue() : -1;
    Map<String, Object> data = Json.map(value.get("data"));
    ForgeChatEvent event = new ForgeChatEvent(Json.string(value, "type"), version, Json.string(value, "conversationId"),
        Json.string(value, "turnId"), Json.string(value, "messageId"), Json.string(value, "eventId"),
        Json.string(value, "sequence"), Json.string(value, "createdAt"), data);
    if (event.version != 1 || event.type == null || event.turnId == null || event.data == null) {
      throw new ForgeStreamError("Invalid Forge chat event.", "stream_event_invalid");
    }
    return event;
  }

  public boolean settled() {
    return "turn.completed".equals(type) || "turn.failed".equals(type) || "turn.canceled".equals(type) || "turn.requires_action".equals(type);
  }
  @Override public Map<String, Object> toJson() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("type", type); result.put("version", version); result.put("conversationId", conversationId);
    result.put("turnId", turnId); result.put("messageId", messageId); result.put("eventId", eventId);
    result.put("sequence", sequence); result.put("createdAt", createdAt); result.put("data", data); return result;
  }
}
