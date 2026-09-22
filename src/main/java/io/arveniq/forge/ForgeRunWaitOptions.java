package io.arveniq.forge;

import java.time.Duration;
import java.util.function.Consumer;

/** Polling controls for an asynchronous developer run. */
public final class ForgeRunWaitOptions {
  private final String requestId;
  private final Duration pollInterval;
  private final Duration timeout;
  private final Consumer<ForgeDeveloperRun> onPoll;

  private ForgeRunWaitOptions(Builder builder) {
    requestId = builder.requestId;
    pollInterval = builder.pollInterval;
    timeout = builder.timeout;
    onPoll = builder.onPoll;
    if (pollInterval.isZero() || pollInterval.isNegative()) throw new IllegalArgumentException("pollInterval must be positive.");
    if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive.");
  }
  public String requestId() { return requestId; }
  public Duration pollInterval() { return pollInterval; }
  public Duration timeout() { return timeout; }
  public Consumer<ForgeDeveloperRun> onPoll() { return onPoll; }
  public static ForgeRunWaitOptions defaults() { return new Builder().build(); }
  public static Builder builder() { return new Builder(); }
  public static final class Builder {
    private String requestId;
    private Duration pollInterval = Duration.ofSeconds(1);
    private Duration timeout = Duration.ofSeconds(60);
    private Consumer<ForgeDeveloperRun> onPoll;
    public Builder requestId(String value) { requestId = value; return this; }
    public Builder pollInterval(Duration value) { pollInterval = value; return this; }
    public Builder timeout(Duration value) { timeout = value; return this; }
    public Builder onPoll(Consumer<ForgeDeveloperRun> value) { onPoll = value; return this; }
    public ForgeRunWaitOptions build() { return new ForgeRunWaitOptions(this); }
  }
}
