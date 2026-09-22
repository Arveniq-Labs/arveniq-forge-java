package io.arveniq.forge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable audit policy submitted with a governed tool handler. */
public record AuditPolicy(
    boolean auditOnCall, boolean auditInputMetadata, boolean auditOutputMetadata,
    List<String> redactInputFields, List<String> redactOutputFields,
    TraceVisibility traceVisibility, String retentionPolicyId) implements Json.WritableJson {
  public AuditPolicy {
    redactInputFields = redactInputFields == null ? List.of() : List.copyOf(redactInputFields);
    redactOutputFields = redactOutputFields == null ? List.of() : List.copyOf(redactOutputFields);
    traceVisibility = traceVisibility == null ? TraceVisibility.METADATA : traceVisibility;
  }
  @Override public Map<String, Object> toJson() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("audit_on_call", auditOnCall); result.put("audit_input_metadata", auditInputMetadata);
    result.put("audit_output_metadata", auditOutputMetadata); result.put("redact_input_fields", redactInputFields);
    result.put("redact_output_fields", redactOutputFields); result.put("trace_visibility", traceVisibility);
    result.put("retention_policy_id", retentionPolicyId); return result;
  }
}
