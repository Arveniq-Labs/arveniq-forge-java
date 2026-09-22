package io.arveniq.forge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Server-side client for Forge discovery, runs, and streamed conversations. */
public final class ForgeDeveloperClient extends ForgeServerClient {
  public ForgeDeveloperClient(ForgeClientOptions options) { super(options); }

  public List<ForgeDeveloperAgent> listAgents() { return listAgents(ForgeRequestOptions.defaults()); }
  public List<ForgeDeveloperAgent> listAgents(ForgeRequestOptions options) {
    return items(request("/developer/v1/agents", "GET", null, options)).stream().map(ForgeDeveloperAgent::from).toList();
  }
  public ForgeDeveloperAgent getAgent(String agentId) { return getAgent(agentId, ForgeRequestOptions.defaults()); }
  public ForgeDeveloperAgent getAgent(String agentId, ForgeRequestOptions options) {
    return ForgeDeveloperAgent.from(request("/developer/v1/agents/" + requiredId(agentId, "agentId"), "GET", null, options));
  }
  public List<ForgeDeveloperWorkflow> listWorkflows() { return listWorkflows(ForgeRequestOptions.defaults()); }
  public List<ForgeDeveloperWorkflow> listWorkflows(ForgeRequestOptions options) {
    return items(request("/developer/v1/workflows", "GET", null, options)).stream().map(ForgeDeveloperWorkflow::from).toList();
  }
  public ForgeDeveloperRun triggerAgentRun(String agentId, ForgeDeveloperRunInput input) {
    return triggerAgentRun(agentId, input, ForgeRequestOptions.defaults());
  }
  public ForgeDeveloperRun triggerAgentRun(String agentId, ForgeDeveloperRunInput input, ForgeRequestOptions options) {
    return ForgeDeveloperRun.from(request("/developer/v1/agents/" + requiredId(agentId, "agentId") + "/runs", "POST", input == null ? Map.of() : input, options));
  }
  public ForgeDeveloperRun triggerWorkflowRun(String workflowId, ForgeDeveloperRunInput input) {
    return triggerWorkflowRun(workflowId, input, ForgeRequestOptions.defaults());
  }
  public ForgeDeveloperRun triggerWorkflowRun(String workflowId, ForgeDeveloperRunInput input, ForgeRequestOptions options) {
    return ForgeDeveloperRun.from(request("/developer/v1/workflows/" + requiredId(workflowId, "workflowId") + "/runs", "POST", input == null ? Map.of() : input, options));
  }
  public List<ForgeDeveloperRun> listRuns() { return listRuns(ForgeRequestOptions.defaults()); }
  public List<ForgeDeveloperRun> listRuns(ForgeRequestOptions options) {
    return items(request("/developer/v1/runs", "GET", null, options)).stream().map(ForgeDeveloperRun::from).toList();
  }
  public ForgeDeveloperRun getRun(String runId) { return getRun(runId, ForgeRequestOptions.defaults()); }
  public ForgeDeveloperRun getRun(String runId, ForgeRequestOptions options) {
    return ForgeDeveloperRun.from(request("/developer/v1/runs/" + requiredId(runId, "runId"), "GET", null, options));
  }
  public Map<String, Object> getRunTrace(String runId) { return getRunTrace(runId, ForgeRequestOptions.defaults()); }
  public Map<String, Object> getRunTrace(String runId, ForgeRequestOptions options) {
    return request("/developer/v1/runs/" + requiredId(runId, "runId") + "/trace", "GET", null, options);
  }
  public List<ForgeRateLimitState> getRateLimits() { return getRateLimits(ForgeRequestOptions.defaults()); }
  public List<ForgeRateLimitState> getRateLimits(ForgeRequestOptions options) {
    return Json.list(request("/developer/v1/rate-limit", "GET", null, options).get("limits")).stream().map(Json::map).map(ForgeRateLimitState::from).toList();
  }

  public ForgeConversation createConversation(String agentId) { return createConversation(agentId, ForgeRequestOptions.defaults()); }
  public ForgeConversation createConversation(String agentId, ForgeRequestOptions options) {
    return ForgeConversation.from(request("/developer/v1/conversations", "POST", Map.of("agentId", requiredRawId(agentId, "agentId")), options));
  }
  public ForgeConversationSnapshot getConversation(String conversationId) { return getConversation(conversationId, ForgeRequestOptions.defaults()); }
  public ForgeConversationSnapshot getConversation(String conversationId, ForgeRequestOptions options) {
    return ForgeConversationSnapshot.from(request("/developer/v1/conversations/" + requiredId(conversationId, "conversationId"), "GET", null, options));
  }
  public Map<String, Object> cancelConversationTurn(String conversationId, String turnId) { return cancelConversationTurn(conversationId, turnId, ForgeRequestOptions.defaults()); }
  public Map<String, Object> cancelConversationTurn(String conversationId, String turnId, ForgeRequestOptions options) {
    return request("/developer/v1/conversations/" + requiredId(conversationId, "conversationId") + "/turns/" + requiredId(turnId, "turnId") + "/cancel", "POST", null, options);
  }

  /** Streams each event as it is received; retries preserve the accepted turn and replay cursor. */
  public void streamMessage(String conversationId, String clientMessageId, String message, Consumer<ForgeChatEvent> receiver) {
    streamMessage(conversationId, clientMessageId, message, ForgeChatStreamOptions.defaults(), receiver);
  }
  public void streamMessage(String conversationId, String clientMessageId, String message, ForgeChatStreamOptions options, Consumer<ForgeChatEvent> receiver) {
    if (clientMessageId == null || clientMessageId.isBlank()) throw new IllegalArgumentException("clientMessageId is required for safe retries.");
    if (message == null || message.isBlank()) throw new IllegalArgumentException("message is required.");
    Map<String, Object> input = Map.of("clientMessageId", clientMessageId, "message", message);
    stream(conversationId, null, input, options, receiver);
  }
  public void streamConversationTurn(String conversationId, String turnId, Consumer<ForgeChatEvent> receiver) {
    streamConversationTurn(conversationId, turnId, ForgeChatStreamOptions.defaults(), receiver);
  }
  public void streamConversationTurn(String conversationId, String turnId, ForgeChatStreamOptions options, Consumer<ForgeChatEvent> receiver) {
    stream(conversationId, requiredRawId(turnId, "turnId"), null, options, receiver);
  }

  public ForgeDeveloperRun waitForRun(String runId) { return waitForRun(runId, ForgeRunWaitOptions.defaults()); }
  public ForgeDeveloperRun waitForRun(String runId, ForgeRunWaitOptions options) {
    long deadline = System.nanoTime() + options.timeout().toNanos();
    while (true) {
      ForgeDeveloperRun run = getRun(runId, ForgeRequestOptions.builder().requestId(options.requestId()).build());
      if (options.onPoll() != null) options.onPoll().accept(run);
      if (run.terminal()) return run;
      if (System.nanoTime() >= deadline) throw new IllegalStateException("Timed out waiting for Forge run " + runId + " after " + options.timeout().toMillis() + " ms.");
      sleep(options.pollInterval());
    }
  }

  private void stream(String conversationId, String turnId, Map<String, Object> input, ForgeChatStreamOptions options, Consumer<ForgeChatEvent> receiver) {
    if (receiver == null) throw new IllegalArgumentException("receiver is required.");
    String rawConversationId = requiredRawId(conversationId, "conversationId");
    String encodedConversationId = encodeSegment(rawConversationId);
    String[] activeTurnId = {turnId};
    String[] cursor = {options.afterEventId()};
    BigInteger[] sequence = {null};
    for (int attempt = 0; ; attempt++) {
      try {
        String path = activeTurnId[0] == null
            ? "/developer/v1/conversations/" + encodedConversationId + "/messages/stream"
            : "/developer/v1/conversations/" + encodedConversationId + "/turns/" + encodeSegment(activeTurnId[0]) + "/events/stream";
        HttpResponse<InputStream> response = streamRequest(path, activeTurnId[0] == null ? "POST" : "GET", activeTurnId[0] == null ? input : null, cursor[0], options);
        String contentType = response.headers().firstValue("content-type").orElse("");
        if (!contentType.toLowerCase().contains("text/event-stream")) {
          closeQuietly(response.body());
          throw new ForgeStreamError("Expected an SSE response from Forge.", "stream_content_type_invalid");
        }
        final String expectedTurn = activeTurnId[0];
        boolean settled;
        settled = readEvents(response.body(), event -> {
          if (!rawConversationId.equals(event.conversationId()) || (expectedTurn != null && !expectedTurn.equals(event.turnId()))) {
            throw new ForgeStreamError("Unexpected stream identity.", "stream_event_invalid");
          }
          if (event.sequence() != null) {
            BigInteger incoming;
            try { incoming = new BigInteger(event.sequence()); } catch (NumberFormatException exception) { throw new ForgeStreamError("Invalid event sequence.", "stream_event_invalid"); }
            if (sequence[0] != null && incoming.compareTo(sequence[0]) <= 0) return;
            sequence[0] = incoming;
          }
          activeTurnId[0] = requiredRawId(event.turnId(), "turnId");
          if (event.eventId() != null) cursor[0] = event.eventId();
          receiver.accept(event);
        });
        if (settled) return;
        throw new ForgeStreamError("Stream ended before the turn settled.");
      } catch (RuntimeException error) {
        if (!retryable(error) || attempt >= options.maxReconnects()) throw error;
        sleep(backoff(options.reconnectDelay(), attempt));
      }
    }
  }

  private static boolean readEvents(InputStream input, Consumer<ForgeChatEvent> receiver) {
    List<String> lines = new ArrayList<>(); int frameBytes = 0; boolean settled = false;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        frameBytes += line.length();
        if (frameBytes > 2_000_000) throw new ForgeStreamError("SSE event exceeds the supported size.", "stream_event_too_large");
        if (!line.isEmpty()) { lines.add(line); continue; }
        ForgeChatEvent event = dispatch(lines); lines.clear(); frameBytes = 0;
        if (event == null) continue;
        receiver.accept(event); if (event.settled()) settled = true;
      }
      return settled;
    } catch (IOException exception) { throw new ForgeStreamError("Forge stream was interrupted.", "developer_stream_interrupted"); }
  }
  private static ForgeChatEvent dispatch(List<String> lines) {
    StringBuilder data = new StringBuilder();
    for (String line : lines) if (line.startsWith("data:")) {
      if (data.length() > 0) data.append('\n'); data.append(line.substring(5).replaceFirst("^ ", ""));
    }
    if (data.length() == 0) return null;
    Map<String, Object> raw;
    try { raw = Json.object(data.toString()); } catch (IllegalArgumentException exception) { throw new ForgeStreamError("Invalid SSE JSON.", "stream_event_invalid"); }
    if ("error".equals(Json.string(raw, "type"))) throw new ForgeStreamError(Json.string(raw, "message") == null ? "Forge stream interrupted." : Json.string(raw, "message"), Json.string(raw, "code") == null ? "stream_interrupted" : Json.string(raw, "code"));
    return ForgeChatEvent.from(raw);
  }
  private static boolean retryable(RuntimeException error) {
    if (error instanceof ForgeApiError api) return api.status() == 429 || api.status() >= 500;
    return error instanceof ForgeStreamError stream && ("stream_interrupted".equals(stream.code()) || "developer_stream_interrupted".equals(stream.code()));
  }
  private static Duration backoff(Duration initial, int attempt) {
    long multiplier = 1L << Math.min(attempt, 20);
    return Duration.ofMillis(Math.min(10_000, Math.multiplyExact(initial.toMillis(), multiplier)));
  }
  private static void sleep(Duration delay) {
    try { Thread.sleep(delay.toMillis()); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new ForgeStreamError("Forge operation was interrupted.", "stream_interrupted"); }
  }
  private static void closeQuietly(InputStream input) { try { input.close(); } catch (IOException ignored) { } }
  private static List<Map<String, Object>> items(Map<String, Object> response) {
    return Json.list(response.get("items")).stream().map(Json::map).toList();
  }
  private static String requiredRawId(String value, String name) {
    if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required.");
    return value.trim();
  }
}
