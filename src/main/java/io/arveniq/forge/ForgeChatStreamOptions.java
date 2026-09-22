package io.arveniq.forge;

import java.time.Duration;

/** Reconnection options for a Forge conversation event stream. */
public final class ForgeChatStreamOptions {
  private final String requestId;
  private final String afterEventId;
  private final int maxReconnects;
  private final Duration reconnectDelay;
  private final Duration timeout;

  private ForgeChatStreamOptions(Builder builder) {
    requestId = builder.requestId;
    afterEventId = builder.afterEventId;
    maxReconnects = builder.maxReconnects;
    reconnectDelay = builder.reconnectDelay;
    timeout = builder.timeout;
    if (maxReconnects < 0) throw new IllegalArgumentException("maxReconnects must be nonnegative.");
    if (reconnectDelay.isNegative() || reconnectDelay.isZero()) throw new IllegalArgumentException("reconnectDelay must be positive.");
  }

  public String requestId() { return requestId; }
  public String afterEventId() { return afterEventId; }
  public int maxReconnects() { return maxReconnects; }
  public Duration reconnectDelay() { return reconnectDelay; }
  public Duration timeout() { return timeout; }
  public static ForgeChatStreamOptions defaults() { return new Builder().build(); }
  public static Builder builder() { return new Builder(); }

  public static final class Builder {
    private String requestId;
    private String afterEventId;
    private int maxReconnects = 5;
    private Duration reconnectDelay = Duration.ofMillis(500);
    private Duration timeout;
    public Builder requestId(String value) { requestId = value; return this; }
    public Builder afterEventId(String value) { afterEventId = value; return this; }
    public Builder maxReconnects(int value) { maxReconnects = value; return this; }
    public Builder reconnectDelay(Duration value) { reconnectDelay = value; return this; }
    public Builder timeout(Duration value) { timeout = value; return this; }
    public ForgeChatStreamOptions build() { return new ForgeChatStreamOptions(this); }
  }
}
