package io.arveniq.forge;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Shared, server-only HTTP transport. Forge credentials never leave this class. */
abstract class ForgeServerClient {
  private final URI baseUrl;
  private final HttpClient httpClient;
  private final String apiKey;
  private final String userAgent;
  private final Duration defaultTimeout;

  ForgeServerClient(ForgeClientOptions options) {
    baseUrl = normalizeBaseUrl(options.baseUrl());
    httpClient = options.httpClient();
    apiKey = normalizeApiKey(options.apiKey());
    userAgent = blankToNull(options.userAgent());
    defaultTimeout = options.requestTimeout();
    if (defaultTimeout.isNegative() || defaultTimeout.isZero()) throw new IllegalArgumentException("requestTimeout must be positive.");
  }

  protected Map<String, Object> request(String path, String method, Object body, ForgeRequestOptions options) {
    HttpResponse<String> response;
    String requestId = requestId(options == null ? null : options.requestId());
    try {
      response = httpClient.send(build(path, method, body, "application/json", null, requestId,
          options == null ? null : options.timeout()), HttpResponse.BodyHandlers.ofString());
    } catch (IOException exception) {
      throw new IllegalStateException("Could not connect to Forge.", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Forge request was interrupted.", exception);
    }
    ensureSuccess(response.statusCode(), response.body(), response.headers().firstValue("x-request-id").orElse(requestId));
    if (response.statusCode() == 204 || response.body().isBlank()) return Map.of();
    return Json.object(response.body());
  }

  protected HttpResponse<InputStream> streamRequest(
      String path, String method, Object body, String lastEventId, ForgeChatStreamOptions options) {
    String requestId = requestId(options.requestId());
    try {
      HttpResponse<InputStream> response = httpClient.send(build(path, method, body, "text/event-stream", lastEventId,
          requestId, options.timeout()), HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String responseBody;
        try (InputStream bodyStream = response.body()) { responseBody = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8); }
        ensureSuccess(response.statusCode(), responseBody, response.headers().firstValue("x-request-id").orElse(requestId));
      }
      return response;
    } catch (ForgeApiError exception) {
      throw exception;
    } catch (IOException exception) {
      throw new ForgeStreamError("Forge stream was interrupted.", "developer_stream_interrupted");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ForgeStreamError("Forge stream was interrupted.", "developer_stream_interrupted");
    }
  }

  protected static String requiredId(String value, String name) {
    if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required.");
    return encodeSegment(value.trim());
  }

  protected static String encodeSegment(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private HttpRequest build(String path, String method, Object body, String accept, String lastEventId, String requestId, Duration timeout) {
    if (!path.startsWith("/")) throw new IllegalArgumentException("Forge API paths must start with '/'.");
    HttpRequest.Builder request = HttpRequest.newBuilder(baseUrl.resolve(path.substring(1)))
        .header("accept", accept)
        .header("authorization", "Bearer " + apiKey)
        .header("x-request-id", requestId)
        .timeout(timeout == null ? defaultTimeout : timeout);
    if (userAgent != null) request.header("user-agent", userAgent);
    if (lastEventId != null && !lastEventId.isBlank()) request.header("last-event-id", lastEventId);
    if (body == null) return request.method(method, HttpRequest.BodyPublishers.noBody()).build();
    return request.header("content-type", "application/json")
        .method(method, HttpRequest.BodyPublishers.ofString(Json.stringify(body), StandardCharsets.UTF_8)).build();
  }

  private static void ensureSuccess(int status, String body, String requestId) {
    if (status < 200 || status >= 300) throw new ForgeApiError("Forge API request failed with " + status + ".", status, body, requestId);
  }

  private static URI normalizeBaseUrl(String value) {
    if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("baseUrl is required.");
    URI candidate;
    try { candidate = URI.create(value.trim()); } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("baseUrl must be an absolute HTTP(S) URL."); }
    if (candidate.getScheme() == null || candidate.getHost() == null || (!"https".equalsIgnoreCase(candidate.getScheme()) && !"http".equalsIgnoreCase(candidate.getScheme()))) {
      throw new IllegalArgumentException("baseUrl must be an absolute HTTP(S) URL.");
    }
    if (candidate.getUserInfo() != null || candidate.getQuery() != null || candidate.getFragment() != null) {
      throw new IllegalArgumentException("baseUrl must not contain credentials, a query string, or a fragment.");
    }
    if ("http".equalsIgnoreCase(candidate.getScheme()) && !isLoopback(candidate.getHost())) {
      throw new IllegalArgumentException("baseUrl must use HTTPS unless it targets a loopback host.");
    }
    String normalized = candidate.toString();
    return URI.create(normalized.endsWith("/") ? normalized : normalized + "/");
  }

  private static boolean isLoopback(String host) {
    String value = host.toLowerCase().replaceAll("\\.$", "");
    return "localhost".equals(value) || "::1".equals(value) || value.matches("127(?:\\.\\d{1,3}){3}");
  }
  private static String normalizeApiKey(String value) {
    String result = value == null ? "" : value.trim().replaceFirst("(?i)^Bearer\\s+", "");
    if (result.isEmpty()) throw new IllegalArgumentException("apiKey is required.");
    return result;
  }
  private static String requestId(String value) { return value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim(); }
  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
