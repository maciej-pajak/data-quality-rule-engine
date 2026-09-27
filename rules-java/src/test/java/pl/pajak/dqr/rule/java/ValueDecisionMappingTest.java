package pl.pajak.dqr.rule.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.pajak.rule.engine.Decision;
import pl.pajak.rule.engine.rule.DecisionMapping;

class ValueDecisionMappingTest {

  @Test
  void resolvesAValueListedInTheTable() {
    final var mapping =
        new ValueDecisionMapping(Map.of("blocked", Decision.INVALID), Optional.empty());

    assertThat(mapping.decisionOf("blocked"))
        .isEqualTo(new DecisionMapping.Resolution.Matched(Decision.INVALID));
  }

  @Test
  void fallsBackToTheDefaultDecision() {
    final var mapping =
        new ValueDecisionMapping(
            Map.of("blocked", Decision.INVALID), Optional.of(Decision.NOT_APPLICABLE));

    assertThat(mapping.decisionOf("ok"))
        .isEqualTo(new DecisionMapping.Resolution.Defaulted(Decision.NOT_APPLICABLE));
  }

  @Test
  void reportsAnUnmappedValueWhenNoDefaultIsDeclared() {
    final var mapping =
        new ValueDecisionMapping(Map.of("blocked", Decision.INVALID), Optional.empty());

    assertThat(mapping.decisionOf("ok")).isEqualTo(new DecisionMapping.Resolution.Unmapped("ok"));
  }

  @Test
  void keepsTheTableImmutableAgainstLaterChangesToTheSource() {
    final var source = new java.util.HashMap<String, Decision>();
    source.put("blocked", Decision.INVALID);
    final var mapping = new ValueDecisionMapping(source, Optional.empty());

    source.put("ok", Decision.VALID);

    assertThat(mapping.decisionOf("ok")).isEqualTo(new DecisionMapping.Resolution.Unmapped("ok"));
  }
}
