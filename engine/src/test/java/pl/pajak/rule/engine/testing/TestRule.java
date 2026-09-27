package pl.pajak.rule.engine.testing;

import java.util.List;
import java.util.Optional;
import pl.pajak.rule.engine.Decision;
import pl.pajak.rule.engine.rule.ComputationResult;
import pl.pajak.rule.engine.rule.CountryScope;
import pl.pajak.rule.engine.rule.DecisionMapping;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleSeverity;
import pl.pajak.rule.engine.rule.RuleStatus;
import pl.pajak.rule.engine.rule.ValueLogic;

/**
 * A rule assembled for a test: every attribute has a harmless default, so a test states only what
 * it is about.
 */
public record TestRule(
    String id,
    String label,
    RuleStatus status,
    RuleSeverity severity,
    List<String> categories,
    Optional<CountryScope> countryScope,
    ValueLogic logic,
    DecisionMapping decisionMapping)
    implements Rule {

  public static Builder builder(String id) {
    return new Builder(id);
  }

  /** A released rule that computes one value and maps it to {@link Decision#VALID}. */
  public static Rule of(String id) {
    return builder(id).build();
  }

  public static final class Builder {

    private final String id;

    private String label;
    private RuleStatus status = RuleStatus.RELEASED;
    private RuleSeverity severity = RuleSeverity.ERROR;
    private List<String> categories = List.of();
    private Optional<CountryScope> countryScope = Optional.empty();
    private ValueLogic logic = record -> new ComputationResult.ComputedValue("value");
    private DecisionMapping decisionMapping =
        value -> new DecisionMapping.Resolution.Matched(Decision.VALID);

    private Builder(String id) {
      this.id = id;
      this.label = id;
    }

    public Builder withLabel(String label) {
      this.label = label;
      return this;
    }

    public Builder withStatus(RuleStatus status) {
      this.status = status;
      return this;
    }

    public Builder withSeverity(RuleSeverity severity) {
      this.severity = severity;
      return this;
    }

    public Builder withCategories(String... categories) {
      return withCategories(List.of(categories));
    }

    public Builder withCategories(List<String> categories) {
      this.categories = List.copyOf(categories);
      return this;
    }

    public Builder withCountryScope(CountryScope countryScope) {
      this.countryScope = Optional.of(countryScope);
      return this;
    }

    public Builder withoutCountryScope() {
      this.countryScope = Optional.empty();
      return this;
    }

    public Builder withLogic(ValueLogic logic) {
      this.logic = logic;
      return this;
    }

    /** Logic that computes {@code value} for every record. */
    public Builder computing(String value) {
      return withLogic(record -> new ComputationResult.ComputedValue(value));
    }

    /** Logic that reports a failure it detected itself. */
    public Builder failing(String message) {
      return withLogic(record -> new ComputationResult.ComputationError(message));
    }

    /** Logic that throws instead of returning a result. */
    public Builder throwing(RuntimeException cause) {
      return withLogic(
          record -> {
            throw cause;
          });
    }

    public Builder withDecisionMapping(DecisionMapping decisionMapping) {
      this.decisionMapping = decisionMapping;
      return this;
    }

    /** A mapping that lists every computed value under {@code decision}. */
    public Builder deciding(Decision decision) {
      return withDecisionMapping(value -> new DecisionMapping.Resolution.Matched(decision));
    }

    /** A mapping that falls back to {@code decision} for every computed value. */
    public Builder defaultingTo(Decision decision) {
      return withDecisionMapping(value -> new DecisionMapping.Resolution.Defaulted(decision));
    }

    /** A mapping that covers no value and declares no default. */
    public Builder mappingNothing() {
      return withDecisionMapping(DecisionMapping.Resolution.Unmapped::new);
    }

    public Rule build() {
      return new TestRule(
          id, label, status, severity, categories, countryScope, logic, decisionMapping);
    }
  }
}
