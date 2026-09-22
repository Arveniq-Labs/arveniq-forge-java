package io.arveniq.forge;

import java.time.Duration;

/** Per-request correlation and timeout controls. */
public final class ForgeRequestOptions {
  private final String requestId;
  private final Duration timeout;

  private ForgeRequestOptions(Builder builder) { this.requestId = builder.requestId; this.timeout = builder.timeout; }
  public String requestId() { return requestId; }
  public Duration timeout() { return timeout; }
  public static ForgeRequestOptions defaults() { return new Builder().build(); }
  public static Builder builder() { return new Builder(); }

  public static final class Builder {
    private String requestId;
    private Duration timeout;
    public Builder requestId(String value) { requestId = value; return this; }
    public Builder timeout(Duration value) { timeout = value; return this; }
    public ForgeRequestOptions build() { return new ForgeRequestOptions(this); }
  }
}
