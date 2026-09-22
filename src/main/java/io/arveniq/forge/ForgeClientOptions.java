package io.arveniq.forge;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/** Configuration for a server-side Forge API client. */
public final class ForgeClientOptions {
  private final String baseUrl;
  private final String apiKey;
  private final HttpClient httpClient;
  private final String userAgent;
  private final Duration requestTimeout;

  private ForgeClientOptions(Builder builder) {
    this.baseUrl = Objects.requireNonNull(builder.baseUrl, "baseUrl is required.");
    this.apiKey = Objects.requireNonNull(builder.apiKey, "apiKey is required.");
    this.httpClient = builder.httpClient == null ? HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build() : builder.httpClient;
    this.userAgent = builder.userAgent;
    this.requestTimeout = builder.requestTimeout == null ? Duration.ofSeconds(60) : builder.requestTimeout;
  }

  public String baseUrl() { return baseUrl; }
  public String apiKey() { return apiKey; }
  public HttpClient httpClient() { return httpClient; }
  public String userAgent() { return userAgent; }
  public Duration requestTimeout() { return requestTimeout; }
  public static Builder builder() { return new Builder(); }

  public static final class Builder {
    private String baseUrl;
    private String apiKey;
    private HttpClient httpClient;
    private String userAgent;
    private Duration requestTimeout;
    public Builder baseUrl(String value) { baseUrl = value; return this; }
    public Builder apiKey(String value) { apiKey = value; return this; }
    public Builder httpClient(HttpClient value) { httpClient = value; return this; }
    public Builder userAgent(String value) { userAgent = value; return this; }
    public Builder requestTimeout(Duration value) { requestTimeout = value; return this; }
    public ForgeClientOptions build() { return new ForgeClientOptions(this); }
  }
}
