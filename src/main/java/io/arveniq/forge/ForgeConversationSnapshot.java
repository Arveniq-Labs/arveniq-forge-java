package io.arveniq.forge;

import java.util.List;
import java.util.Map;

/** A conversation and its latest server-authorized turns. */
public record ForgeConversationSnapshot(String id, String agentId, String createdAt, List<Map<String, Object>> turns) {
  public static ForgeConversationSnapshot from(Map<String, Object> value) {
    return new ForgeConversationSnapshot(Json.string(value, "id"), Json.string(value, "agentId"), Json.string(value, "createdAt"),
        Json.list(value.get("turns")).stream().map(Json::map).toList());
  }
}
