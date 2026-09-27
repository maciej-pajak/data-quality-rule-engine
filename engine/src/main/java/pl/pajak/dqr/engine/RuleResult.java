package pl.pajak.dqr.engine;

import java.util.Map;
import pl.pajak.dqr.engine.rule.RuleSeverity;

/**
 * The outcome of evaluating one rule against one record.
 *
 * <p>A rule that fails yields a {@link RuleError} instead. Rules whose country scope does not cover
 * the record are not evaluated at all, so they produce neither.
 *
 * <p>Instances are immutable.
 */
public sealed interface RuleResult permits RuleResultImpl {

  /**
   * Returns the identifier of the record that was evaluated.
   *
   * @return the record identifier, as reported by {@link PartnerRecord#refId()}
   */
  String recordRefId();

  /**
   * Returns the identifier of the rule that was evaluated.
   *
   * @return the rule identifier
   */
  String ruleId();

  /**
   * Returns the human-readable name of the rule that was evaluated.
   *
   * <p>Carried with the outcome so that a report can name the rule without consulting the catalog
   * again. It is a property of the rule, not of this outcome.
   *
   * @return the rule's label, as reported by {@link pl.pajak.dqr.engine.rule.Rule#label()}
   */
  String ruleLabel();

  /**
   * Returns the decision the rule's mapping resolved for the computed value.
   *
   * @return the decision
   */
  Decision decision();

  /**
   * Returns the severity declared by the rule.
   *
   * <p>It is a property of the rule, not of this outcome: it is the same for every record the rule
   * is evaluated against, whatever the decision.
   *
   * @return the rule's severity
   */
  RuleSeverity severity();

  /**
   * Returns the trace of how the decision was reached.
   *
   * @return the provenance of this result
   */
  Provenance provenance();

  /**
   * Explains a single result: the value the rule computed, how it became a decision, and the record
   * data it was computed from.
   */
  interface Provenance {

    /**
     * Returns the value the rule's logic computed for the record.
     *
     * @return the computed value, the input of the rule's decision mapping
     */
    String computedValue();

    /**
     * Returns a human-readable explanation of how the computed value became the decision.
     *
     * @return the explanation, for example that the value is mapped or that the rule's default
     *     applies
     */
    String description();

    /**
     * Returns the record fields the rule read while computing the value, in the order they were
     * read.
     *
     * <p>A field the record does not carry is present with a {@code null} value: the rule saw its
     * absence, which is part of what explains the decision. Repeated reads of the same field appear
     * once.
     *
     * @return an unmodifiable map of field name to value
     */
    Map<String, String> accessedFields();
  }
}
