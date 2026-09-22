package io.arveniq.forge;

/** Approval policy required before a tool handler executes. */
public enum ApprovalRequirement { NONE("none"), ALWAYS("always"), POLICY_BASED("policy_based"), HIGH_RISK_ONLY("high_risk_only");
  private final String wire; ApprovalRequirement(String wire) { this.wire = wire; } @Override public String toString() { return wire; } }
