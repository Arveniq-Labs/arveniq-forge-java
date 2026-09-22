package io.arveniq.forge;

/** A precise path and reason emitted by manifest validation. */
public record ValidationIssue(String path, String message, Severity severity) {
  public enum Severity { ERROR, WARNING }
}
