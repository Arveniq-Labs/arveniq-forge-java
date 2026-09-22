package io.arveniq.forge;

/** A tool or agent risk classification. */
public enum RiskLevel { LOW("low"), MEDIUM("medium"), HIGH("high"), RESTRICTED("restricted");
  private final String wire; RiskLevel(String wire) { this.wire = wire; } @Override public String toString() { return wire; } }
