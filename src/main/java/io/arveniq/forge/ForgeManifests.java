package io.arveniq.forge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Builders and validation for governed Forge tool-package definitions. */
public final class ForgeManifests {
  private static final Pattern NAMESPACE = Pattern.compile("^[a-z0-9][a-z0-9-]*$");
  private static final Pattern HANDLER_REF = Pattern.compile("^tool://([a-z0-9][a-z0-9-]*)/([a-zA-Z0-9_.:-]+)$");
  private ForgeManifests() {}

  public static AuditPolicy auditPolicy() { return new AuditPolicy(true, true, true, List.of(), List.of(), TraceVisibility.METADATA, "standard"); }
  public static AuditPolicy auditPolicy(List<String> redactInputFields, List<String> redactOutputFields, TraceVisibility visibility, String retentionPolicyId) {
    return new AuditPolicy(true, true, true, redactInputFields, redactOutputFields, visibility, retentionPolicyId == null ? "standard" : retentionPolicyId);
  }
  public static HandlerBuilder handler() { return new HandlerBuilder(); }
  public static ManifestBuilder manifest() { return new ManifestBuilder(); }

  /** Forces read-only execution and a retained minimum audit trail. */
  public static ToolPackageHandlerManifest createReadOnlyHandler(HandlerBuilder input) {
    return input.executionMode(ToolExecutionMode.READ_ONLY).auditPolicy(readOnlyPolicy(input.auditPolicy)).build();
  }
  public static AuditPolicy readOnlyPolicy(AuditPolicy overrides) {
    if (overrides == null) return auditPolicy();
    return new AuditPolicy(true, true, true, overrides.redactInputFields(), overrides.redactOutputFields(), overrides.traceVisibility(), overrides.retentionPolicyId());
  }

  public static ValidationResult validate(ToolPackageManifest manifest) {
    return validateJson(manifest == null ? null : manifest.toJson());
  }
  /** Validates parsed JSON too, which is useful in a deployment CLI or build plugin. */
  public static ValidationResult validateJson(Object candidate) {
    List<ValidationIssue> errors = new ArrayList<>(); List<ValidationIssue> warnings = new ArrayList<>();
    if (!(candidate instanceof Map<?, ?>)) { error(errors, "$", "Manifest must be a JSON object."); return result(errors, warnings); }
    Map<String, Object> manifest = Json.map(candidate);
    String namespace = required(manifest, "namespace", "$.namespace", errors);
    required(manifest, "name", "$.name", errors); required(manifest, "version", "$.version", errors);
    optionalString(manifest, "description", "$.description", errors);
    if (namespace != null && !NAMESPACE.matcher(namespace).matches()) error(errors, "$.namespace", "Namespace must use lowercase letters, numbers, and hyphens.");
    Map<String, Object> runtime = Json.map(manifest.get("runtime"));
    if (runtime.isEmpty()) error(errors, "$.runtime", "Runtime metadata is required.");
    else {
      required(runtime, "entrypoint", "$.runtime.entrypoint", errors);
      if (!Set.of("node", "container", "serverless", "mcp", "internal").contains(wire(runtime.get("type")))) error(errors, "$.runtime.type", "Runtime type is invalid.");
    }
    List<Object> handlers = Json.list(manifest.get("handlers"));
    if (handlers.isEmpty()) { error(errors, "$.handlers", "At least one handler is required."); return result(errors, warnings); }
    Set<String> slugs = new HashSet<>(); Set<String> refs = new HashSet<>();
    for (int index = 0; index < handlers.size(); index++) {
      String path = "$.handlers[" + index + "]";
      Map<String, Object> handler = Json.map(handlers.get(index));
      if (handler.isEmpty()) { error(errors, path, "Handler must be an object."); continue; }
      String reference = required(handler, "handler_ref", path + ".handler_ref", errors);
      String slug = required(handler, "slug", path + ".slug", errors);
      required(handler, "name", path + ".name", errors); optionalString(handler, "description", path + ".description", errors);
      if (reference != null) {
        var match = HANDLER_REF.matcher(reference);
        if (!match.matches()) error(errors, path + ".handler_ref", "Handler ref must match tool://namespace/action_name.");
        else if (namespace != null && !namespace.equals(match.group(1))) error(errors, path + ".handler_ref", "Handler ref namespace must match package namespace.");
        if (!refs.add(reference)) error(errors, path + ".handler_ref", "Handler ref must be unique.");
      }
      if (slug != null && !slugs.add(slug)) error(errors, path + ".slug", "Handler slug must be unique.");
      if (!strings(handler.get("required_scopes"), false)) error(errors, path + ".required_scopes", "Required scopes must be explicit.");
      if (Json.list(handler.get("supported_environments")).isEmpty()) error(errors, path + ".supported_environments", "At least one supported environment is required.");
      else if (Json.list(handler.get("supported_environments")).stream().map(ForgeManifests::wire).anyMatch(value -> !Set.of("DEVELOPMENT", "STAGING", "PRODUCTION").contains(value))) error(errors, path + ".supported_environments", "Supported environments must be valid Forge environments.");
      String execution = wire(handler.get("execution_mode")); String risk = wire(handler.get("risk_level")); String approval = wire(handler.get("approval_requirement"));
      if (!Set.of("read_only", "write", "external_action").contains(execution)) error(errors, path + ".execution_mode", "Execution mode is invalid.");
      if (!Set.of("low", "medium", "high", "restricted").contains(risk)) error(errors, path + ".risk_level", "Risk level is invalid.");
      if (!Set.of("none", "always", "policy_based", "high_risk_only").contains(approval)) error(errors, path + ".approval_requirement", "Approval requirement is invalid.");
      if (!schema(handler.get("input_schema"))) error(errors, path + ".input_schema", "Input schema must be a JSON schema object.");
      if (!schema(handler.get("output_schema"))) error(errors, path + ".output_schema", "Output schema must be a JSON schema object.");
      if ("external_action".equals(execution) && contains(handler.get("supported_environments"), "PRODUCTION") && "none".equals(approval)) error(errors, path + ".approval_requirement", "Production external_action handlers require approval policy.");
      if (("high".equals(risk) || "restricted".equals(risk)) && "none".equals(approval)) error(errors, path + ".approval_requirement", "High/restricted risk handlers require approval policy.");
      audit(handler.get("audit_policy"), path + ".audit_policy", errors);
    }
    return result(errors, warnings);
  }
  public static void assertValid(ToolPackageManifest manifest) {
    ValidationResult result = validate(manifest);
    if (!result.valid()) throw new IllegalArgumentException("Invalid tool package manifest:\n" + result.errors().stream().map(issue -> issue.path() + ": " + issue.message()).reduce("", (left, right) -> left.isEmpty() ? right : left + "\n" + right));
  }

  public static final class HandlerBuilder {
    private String handlerRef; private String slug; private String name; private String description;
    private ToolExecutionMode executionMode = ToolExecutionMode.READ_ONLY; private RiskLevel riskLevel = RiskLevel.MEDIUM;
    private List<String> requiredScopes = List.of(); private List<DeploymentEnvironment> supportedEnvironments = List.of(DeploymentEnvironment.DEVELOPMENT);
    private ApprovalRequirement approvalRequirement = ApprovalRequirement.NONE; private Map<String, Object> inputSchema = Map.of(); private Map<String, Object> outputSchema = Map.of(); private AuditPolicy auditPolicy = ForgeManifests.auditPolicy();
    public HandlerBuilder handlerRef(String value) { handlerRef = value; return this; }
    public HandlerBuilder slug(String value) { slug = value; return this; }
    public HandlerBuilder name(String value) { name = value; return this; }
    public HandlerBuilder description(String value) { description = value; return this; }
    public HandlerBuilder executionMode(ToolExecutionMode value) { executionMode = value; return this; }
    public HandlerBuilder riskLevel(RiskLevel value) { riskLevel = value; return this; }
    public HandlerBuilder requiredScopes(List<String> value) { requiredScopes = value; return this; }
    public HandlerBuilder supportedEnvironments(List<DeploymentEnvironment> value) { supportedEnvironments = value; return this; }
    public HandlerBuilder approvalRequirement(ApprovalRequirement value) { approvalRequirement = value; return this; }
    public HandlerBuilder inputSchema(Map<String, Object> value) { inputSchema = value; return this; }
    public HandlerBuilder outputSchema(Map<String, Object> value) { outputSchema = value; return this; }
    public HandlerBuilder auditPolicy(AuditPolicy value) { auditPolicy = value; return this; }
    public ToolPackageHandlerManifest build() { return new ToolPackageHandlerManifest(handlerRef, slug, name, description, executionMode, riskLevel, requiredScopes, supportedEnvironments, approvalRequirement, inputSchema, outputSchema, auditPolicy); }
  }
  public static final class ManifestBuilder {
    private String name; private String namespace; private String version; private String description; private ToolPackageRuntimeType runtimeType = ToolPackageRuntimeType.NODE; private String entrypoint = "src/index.ts"; private List<ToolPackageHandlerManifest> handlers = List.of();
    public ManifestBuilder name(String value) { name = value; return this; }
    public ManifestBuilder namespace(String value) { namespace = value; return this; }
    public ManifestBuilder version(String value) { version = value; return this; }
    public ManifestBuilder description(String value) { description = value; return this; }
    public ManifestBuilder runtime(ToolPackageRuntimeType type, String value) { runtimeType = type; entrypoint = value; return this; }
    public ManifestBuilder handlers(List<ToolPackageHandlerManifest> value) { handlers = value; return this; }
    public ToolPackageManifest build() { return new ToolPackageManifest(name, namespace, version, description, runtimeType, entrypoint, handlers); }
  }

  private static void audit(Object candidate, String path, List<ValidationIssue> errors) {
    Map<String, Object> policy = Json.map(candidate); if (policy.isEmpty()) { error(errors, path, "Audit policy is required."); return; }
    for (String key : List.of("audit_on_call", "audit_input_metadata", "audit_output_metadata")) if (!Boolean.TRUE.equals(policy.get(key))) error(errors, path + "." + key, key + " must be true for governed tool packages.");
    for (String key : List.of("redact_input_fields", "redact_output_fields")) if (!strings(policy.get(key), true)) error(errors, path + "." + key, key + " must be an array of field names.");
    if (!Set.of("summary", "metadata", "full_safe").contains(wire(policy.get("trace_visibility")))) error(errors, path + ".trace_visibility", "trace_visibility is invalid.");
    Object retention = policy.get("retention_policy_id"); if (retention != null && !(retention instanceof String)) error(errors, path + ".retention_policy_id", "retention_policy_id must be a string or null.");
  }
  private static String required(Map<String, Object> input, String key, String path, List<ValidationIssue> errors) { Object value = input.get(key); if (!(value instanceof String text) || text.isBlank()) { error(errors, path, key + " is required."); return null; } return text; }
  private static void optionalString(Map<String, Object> input, String key, String path, List<ValidationIssue> errors) { if (input.containsKey(key) && input.get(key) != null && !(input.get(key) instanceof String)) error(errors, path, key + " must be a string."); }
  private static boolean strings(Object candidate, boolean permitsEmpty) { List<Object> list = Json.list(candidate); return (permitsEmpty || !list.isEmpty()) && list.stream().allMatch(String.class::isInstance); }
  private static boolean schema(Object candidate) { Map<String, Object> map = Json.map(candidate); Object type = map.get("type"); return !map.isEmpty() && (type instanceof String || type instanceof List<?>); }
  private static boolean contains(Object candidate, String expected) { return Json.list(candidate).stream().map(ForgeManifests::wire).anyMatch(expected::equals); }
  private static String wire(Object value) { return value == null ? "" : value.toString(); }
  private static void error(List<ValidationIssue> errors, String path, String message) { errors.add(new ValidationIssue(path, message, ValidationIssue.Severity.ERROR)); }
  private static ValidationResult result(List<ValidationIssue> errors, List<ValidationIssue> warnings) { return new ValidationResult(errors.isEmpty(), errors, warnings); }
}
