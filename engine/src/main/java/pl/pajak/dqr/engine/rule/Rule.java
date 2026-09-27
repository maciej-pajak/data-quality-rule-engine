package pl.pajak.dqr.engine.rule;

import java.util.List;
import java.util.Optional;

/**
 * One data-quality rule: what it computes about a record, and how that becomes a decision.
 *
 * <p>A rule is split in two on purpose: its {@link #logic()} computes a value from the record's
 * fields, and its {@link #decisionMapping()} turns that value into a decision. The remaining
 * accessors describe the rule rather than drive it - they identify it, select it for a run and
 * accompany its outcomes.
 *
 * <p>Implementations are expected to be immutable: one rule is evaluated against many records.
 */
public interface Rule {

  /**
   * Returns the identifier of this rule.
   *
   * <p>Reported with every outcome the rule produces, so it should be stable and unique within a
   * catalog.
   *
   * @return the rule identifier
   */
  String id();

  /**
   * Returns a human-readable name for this rule.
   *
   * @return the label, for people reading a report
   */
  String label();

  /**
   * Returns where this rule stands in its lifecycle.
   *
   * @return the status a run's rule filter selects on
   */
  RuleStatus status();

  /**
   * Returns how much weight this rule's verdicts carry.
   *
   * @return the severity reported with every result of this rule
   */
  RuleSeverity severity();

  /**
   * Returns the categories this rule belongs to.
   *
   * <p>Free-form labels a run can select rules by.
   *
   * @return the categories, empty when the rule declares none
   */
  List<String> categories();

  /**
   * Returns the countries this rule applies to.
   *
   * @return the country scope, or an empty optional when the rule applies everywhere, as {@link
   *     CountryScope#World} does
   */
  Optional<CountryScope> countryScope();

  /**
   * Returns the computation this rule performs on a record.
   *
   * @return the logic producing the value to be mapped
   */
  ValueLogic logic();

  /**
   * Returns the mapping that turns a computed value into a decision.
   *
   * @return the decision mapping of this rule
   */
  DecisionMapping decisionMapping();
}
