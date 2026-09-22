package io.arveniq.forge;

/** Supported managed-tool package runtime kinds. */
public enum ToolPackageRuntimeType { NODE("node"), CONTAINER("container"), SERVERLESS("serverless"), MCP("mcp"), INTERNAL("internal");
  private final String wire; ToolPackageRuntimeType(String wire) { this.wire = wire; } @Override public String toString() { return wire; } }
