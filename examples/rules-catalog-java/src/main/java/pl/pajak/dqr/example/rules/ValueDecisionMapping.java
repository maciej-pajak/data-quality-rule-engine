package pl.pajak.dqr.example.rules;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import pl.pajak.dqr.engine.Decision;
import pl.pajak.dqr.engine.rule.DecisionMapping;

/**
 * A decision mapping that is pure data: a value → decision table plus an optional fallback for
 * values the table does not list.
 *
 * <p>A value outside the table resolves to {@link DecisionMapping.Resolution.Defaulted} when a
 * fallback is configured, and to {@link Resolution.Unmapped} when it is not.
 */
public record ValueDecisionMapping(
    Map<String, Decision> decisionByValue, Optional<Decision> defaultDecision)
    implements DecisionMapping {

  public ValueDecisionMapping {
    decisionByValue = Map.copyOf(decisionByValue);
    Objects.requireNonNull(defaultDecision, "defaultDecision");
  }

  @Override
  public Resolution decisionOf(String value) {
    final var matched = decisionByValue.get(value);
    if (matched != null) {
      return new Resolution.Matched(matched);
    }
    return defaultDecision
        .<Resolution>map(Resolution.Defaulted::new)
        .orElseGet(() -> new Resolution.Unmapped(value));
  }
}
