package pl.pajak.dqr.engine;

import java.util.Objects;
import java.util.Optional;

/**
 * The cause is held as a plain reference and may be {@code null}; {@link #cause()} is what the API
 * exposes, so the {@link Optional} stays on the boundary instead of becoming a field.
 */
record RuleErrorImpl(String recordRefId, String ruleId, String message, Throwable causeOrNull)
    implements RuleError {

  RuleErrorImpl {
    requireNotBlank(recordRefId, "recordRefId");
    requireNotBlank(ruleId, "ruleId");
    requireNotBlank(message, "message");
  }

  @Override
  public Optional<Throwable> cause() {
    return Optional.ofNullable(causeOrNull);
  }

  private static void requireNotBlank(String value, String name) {
    Objects.requireNonNull(value, name + " cannot be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " cannot be blank");
    }
  }
}
