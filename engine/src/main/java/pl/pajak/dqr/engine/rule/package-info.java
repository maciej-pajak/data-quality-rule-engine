/**
 * What a rule is, and where a run takes its rules from.
 *
 * <p>A {@link Rule} is split in two - {@link ValueLogic} computes a value from a record, {@link
 * DecisionMapping} says what that value means - and reaches a run through a {@link RuleCatalog} the
 * host implements.
 *
 * <p>The engine that evaluates them lives in {@link pl.pajak.dqr.engine}.
 */
package pl.pajak.dqr.engine.rule;
