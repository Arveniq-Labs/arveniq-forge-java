package io.arveniq.forge;

import java.util.Map;

/** Metadata for one asynchronous agent or workflow run. */
public record ForgeDeveloperRun(
    String id, String agentRunId, String legacyExecutionId, Map<String, Object> agent,
    String workflowName, String status, String error, String finalAnswer,
    String createdAt, String startedAt, String completedAt, String updatedAt) {
  public static ForgeDeveloperRun from(Map<String, Object> value) {
    return new ForgeDeveloperRun(
        Json.string(value, "id"), Json.string(value, "agentRunId"), Json.string(value, "legacyExecutionId"),
        Json.map(value.get("agent")), Json.string(value, "workflowName"), Json.string(value, "status"),
        Json.string(value, "error"), Json.string(value, "finalAnswer"), Json.string(value, "createdAt"),
        Json.string(value, "startedAt"), Json.string(value, "completedAt"), Json.string(value, "updatedAt"));
  }

  public boolean terminal() {
    if (status == null) return false;
    return switch (status.trim().toLowerCase()) {
      case "approval_required", "canceled", "cancelled", "completed", "failed", "succeeded", "timed_out", "waiting_approval" -> true;
      default -> false;
    };
  }
}
