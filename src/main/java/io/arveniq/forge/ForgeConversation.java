package io.arveniq.forge;

import java.util.Map;

/** A server-owned Forge chat conversation. */
public record ForgeConversation(String id, String agentId, String createdAt) {
  public static ForgeConversation from(Map<String, Object> value) {
    return new ForgeConversation(Json.string(value, "id"), Json.string(value, "agentId"), Json.string(value, "createdAt"));
  }
}
