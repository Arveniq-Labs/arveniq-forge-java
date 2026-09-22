package io.arveniq.forge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A single governed tool entry in a tool-package manifest. */
public record ToolPackageHandlerManifest(
    String handlerRef, String slug, String name, String description, ToolExecutionMode executionMode,
    RiskLevel riskLevel, List<String> requiredScopes, List<DeploymentEnvironment> supportedEnvironments,
    ApprovalRequirement approvalRequirement, Map<String, Object> inputSchema, Map<String, Object> outputSchema,
    AuditPolicy auditPolicy) implements Json.WritableJson {
  public ToolPackageHandlerManifest {
    description = description == null ? "" : description;
    requiredScopes = requiredScopes == null ? List.of() : List.copyOf(requiredScopes);
    supportedEnvironments = supportedEnvironments == null ? List.of() : List.copyOf(supportedEnvironments);
    inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
    outputSchema = outputSchema == null ? Map.of() : Map.copyOf(outputSchema);
    executionMode = executionMode == null ? ToolExecutionMode.READ_ONLY : executionMode;
    riskLevel = riskLevel == null ? RiskLevel.MEDIUM : riskLevel;
    approvalRequirement = approvalRequirement == null ? ApprovalRequirement.NONE : approvalRequirement;
    auditPolicy = Objects.requireNonNull(auditPolicy, "auditPolicy is required.");
  }
  @Override public Map<String, Object> toJson() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("handler_ref", handlerRef); result.put("slug", slug); result.put("name", name); result.put("description", description);
    result.put("execution_mode", executionMode); result.put("risk_level", riskLevel); result.put("required_scopes", requiredScopes);
    result.put("supported_environments", supportedEnvironments); result.put("approval_requirement", approvalRequirement);
    result.put("input_schema", inputSchema); result.put("output_schema", outputSchema); result.put("audit_policy", auditPolicy.toJson()); return result;
  }
}
