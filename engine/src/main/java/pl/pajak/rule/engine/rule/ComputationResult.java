package pl.pajak.rule.engine.rule;

import java.util.Objects;

public sealed interface ComputationResult
    permits ComputationResult.ComputedValue, ComputationResult.ComputationError {

  record ComputedValue(String value) implements ComputationResult {}

  record ComputationError(String message) implements ComputationResult {
    public ComputationError {
      Objects.requireNonNull(message, "message cannot be null");
    }
  }
}
