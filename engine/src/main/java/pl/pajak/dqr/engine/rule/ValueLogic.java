package pl.pajak.dqr.engine.rule;

import pl.pajak.dqr.engine.PartnerRecord;

/**
 * The computation a rule performs on a record.
 *
 * <p>The logic answers what it observed, not what it concludes: it computes a value such as {@code
 * "ok"}, {@code "bad"} or {@code "blocked"}, and the rule's decision mapping is what assigns a
 * meaning to it.
 *
 * <p>Implementations are given one record at a time and are called on the engine's thread.
 */
public interface ValueLogic {

  /**
   * Computes the value that describes the record for this rule.
   *
   * @param partnerRecord the record being evaluated
   * @return the computed value, or a {@link ComputationResult.ComputationError} explaining why none
   *     could be computed
   * @throws RuntimeException if the computation fails without reporting a reason; the engine
   *     contains it as an error for this record and rule and continues the run
   */
  ComputationResult compute(PartnerRecord partnerRecord);
}
