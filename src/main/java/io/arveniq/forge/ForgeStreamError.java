package io.arveniq.forge;

/** Signals an invalid or unexpectedly interrupted Forge SSE conversation stream. */
public final class ForgeStreamError extends RuntimeException {
  private final String code;
  public ForgeStreamError(String message) { this(message, "stream_interrupted"); }
  public ForgeStreamError(String message, String code) { super(message); this.code = code; }
  public String code() { return code; }
}
