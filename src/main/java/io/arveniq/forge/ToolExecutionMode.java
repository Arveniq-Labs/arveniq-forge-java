package io.arveniq.forge;

/** The authority granted to a governed tool handler. */
public enum ToolExecutionMode { READ_ONLY("read_only"), WRITE("write"), EXTERNAL_ACTION("external_action");
  private final String wire; ToolExecutionMode(String wire) { this.wire = wire; } @Override public String toString() { return wire; } }
