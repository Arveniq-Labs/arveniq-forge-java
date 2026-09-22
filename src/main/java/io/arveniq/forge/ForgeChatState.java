package io.arveniq.forge;

/** Immutable display state that can safely reduce replayed Forge stream events. */
public record ForgeChatState(String text, String turnId, String status, String activity, String lastEventId, String lastSequence) {
  public ForgeChatState { if (text == null) text = ""; }
  public static ForgeChatState empty() { return new ForgeChatState("", null, null, null, null, null); }
}
