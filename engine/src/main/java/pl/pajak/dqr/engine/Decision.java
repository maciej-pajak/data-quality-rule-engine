package pl.pajak.dqr.engine;

/**
 * The verdict a rule reaches about a record.
 *
 * <p>The engine does not interpret a decision, it reports it. Which value a rule reaches is decided
 * by its decision mapping, not by its logic.
 */
public enum Decision {

  /** The record satisfies the rule. */
  VALID,

  /** The record violates the rule. */
  INVALID,

  /** The rule cannot decide on its own; the record needs human judgement. */
  REVIEW,

  /** The rule does not apply to this record, for example because the data it checks is absent. */
  NOT_APPLICABLE
}
