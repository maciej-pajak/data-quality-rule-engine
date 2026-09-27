package pl.pajak.dqr.engine;

import java.util.Optional;

/**
 * A single rule failing on a single record. The run continues; the error is reported here instead
 * of being thrown at the caller.
 *
 * <p>It carries enough to locate the cause: which rule, which record, what went wrong, and the
 * exception when the failure came from one.
 */
public sealed interface RuleError permits RuleErrorImpl {

  /** Reference of the record being evaluated when the rule failed. */
  String recordRefId();

  /** Identifier of the rule that failed. */
  String ruleId();

  /** Human-readable description of the failure, never blank. */
  String message();

  /** The exception behind the failure, empty when the rule reported the failure itself. */
  Optional<Throwable> cause();
}
