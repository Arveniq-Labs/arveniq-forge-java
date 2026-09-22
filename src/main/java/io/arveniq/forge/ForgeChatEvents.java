package io.arveniq.forge;

import java.math.BigInteger;

/** Browser-safe-equivalent stream state reducer for Java renderers and relays. */
public final class ForgeChatEvents {
  private ForgeChatEvents() {}

  public static ForgeChatState reduce(ForgeChatState state, ForgeChatEvent event) {
    ForgeChatState previous = state.turnId() != null && !state.turnId().equals(event.turnId()) ? ForgeChatState.empty() : state;
    if (event.sequence() != null && previous.lastSequence() != null
        && new BigInteger(event.sequence()).compareTo(new BigInteger(previous.lastSequence())) <= 0) return previous;
    String text = previous.text();
    String activity = previous.activity();
    String status = event.data().get("status") == null ? previous.status() : String.valueOf(event.data().get("status"));
    if ("activity.updated".equals(event.type())) activity = string(event, "message");
    if ("response.started".equals(event.type()) && Boolean.TRUE.equals(event.data().get("replace"))) text = "";
    if ("response.delta".equals(event.type())) {
      String delta = string(event, "delta");
      text = Boolean.TRUE.equals(event.data().get("replace")) ? (delta == null ? "" : delta) : text + (delta == null ? "" : delta);
    }
    if ("response.completed".equals(event.type()) && event.data().get("text") != null) text = string(event, "text");
    if (event.type().startsWith("response.") || event.settled()) activity = null;
    return new ForgeChatState(text, event.turnId(), status, activity,
        event.eventId() == null ? previous.lastEventId() : event.eventId(),
        event.sequence() == null ? previous.lastSequence() : event.sequence());
  }

  private static String string(ForgeChatEvent event, String key) {
    Object value = event.data().get(key); return value == null ? null : String.valueOf(value);
  }
}
