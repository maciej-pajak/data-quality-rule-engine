package pl.pajak.dqr.engine.rule;

import java.util.Objects;

/**
 * What a rule's logic produced for one record: either a computed value or the reason none could be
 * computed.
 *
 * <p>Logic that cannot compute a value should return an error rather than throw, so the reason is
 * stated in the rule's own words.
 */
public sealed interface ComputationResult
    permits ComputationResult.ComputedValue, ComputationResult.ComputationError {

  /**
   * The value the logic computed.
   *
   * @param value the value, as text
   */
  record ComputedValue(String value) implements ComputationResult {}

  /**
   * The reason the logic could not compute a value.
   *
   * @param message what went wrong
   */
  record ComputationError(String message) implements ComputationResult {

    /**
     * Checks that the failure is explained.
     *
     * @throws NullPointerException if the message is {@code null}
     */
    public ComputationError {
      Objects.requireNonNull(message, "message cannot be null");
    }
  }
}
