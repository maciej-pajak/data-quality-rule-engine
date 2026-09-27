package pl.pajak.dqr.engine.rule;

/**
 * Where a rule stands in its lifecycle. A run evaluates {@code RELEASED} rules unless told
 * otherwise.
 */
public enum RuleStatus {

  /** Still being worked on, not meant to be evaluated in production. */
  DRAFT,

  /** In force, the rules a run evaluates by default. */
  RELEASED,

  /** Withdrawn, kept in the catalog for reference. */
  DEPRECATED
}
