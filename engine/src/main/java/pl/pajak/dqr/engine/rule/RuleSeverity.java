package pl.pajak.dqr.engine.rule;

/**
 * How much weight a rule's verdict carries.
 *
 * <p>A property of the rule, the same for every record it is evaluated against and whatever it
 * decides. The engine does not act on it, it reports it.
 */
public enum RuleSeverity {

  /** The rule guards data that must be correct. */
  ERROR,

  /** The rule reports a defect that does not make the data unusable. */
  WARNING,

  /** The rule reports an observation, for information only. */
  INFO
}
