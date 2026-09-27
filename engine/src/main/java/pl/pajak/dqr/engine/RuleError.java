package pl.pajak.dqr.engine;

import java.util.Optional;

/**
 * The failure of one rule on one record.
 *
 * <p>A failure is reported, not thrown: the run continues with the remaining rules and records. An
 * error is produced when the rule reports one itself, when it throws, and when the value it
 * computed is covered by neither its decision mapping nor a default decision.
 *
 * <p>Instances are immutable.
 */
public sealed interface RuleError permits RuleErrorImpl {

  /**
   * Returns the identifier of the record being evaluated when the rule failed.
   *
   * @return the record identifier, as reported by {@link PartnerRecord#refId()}
   */
  String recordRefId();

  /**
   * Returns the identifier of the rule that failed.
   *
   * @return the rule identifier
   */
  String ruleId();

  /**
   * Returns a human-readable description of the failure.
   *
   * <p>For a failure the rule threw, the description is derived from the exception's type and
   * message.
   *
   * @return the description, never blank
   */
  String message();

  /**
   * Returns the exception the failure came from.
   *
   * @return the exception, or an empty optional if the failure was reported by the rule rather than
   *     thrown
   */
  Optional<Throwable> cause();
}
