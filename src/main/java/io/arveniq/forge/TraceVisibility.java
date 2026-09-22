package io.arveniq.forge;

/** Safe trace detail made available by a governed handler. */
public enum TraceVisibility { SUMMARY("summary"), METADATA("metadata"), FULL_SAFE("full_safe");
  private final String wire; TraceVisibility(String wire) { this.wire = wire; } @Override public String toString() { return wire; } }
