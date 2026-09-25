package pl.pajak.rule.engine.rule;

public sealed interface ComputationResult
    permits ComputationResult.ComputedValue, ComputationResult.ComputationError {

  record ComputedValue(String value) implements ComputationResult {}

  record ComputationError(String message) implements ComputationResult {}
}
