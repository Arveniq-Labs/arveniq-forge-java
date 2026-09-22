package io.arveniq.forge;

import java.util.List;

/** Result of client-side governed-manifest validation. */
public record ValidationResult(boolean valid, List<ValidationIssue> errors, List<ValidationIssue> warnings) {
  public ValidationResult { errors = List.copyOf(errors); warnings = List.copyOf(warnings); }
}
