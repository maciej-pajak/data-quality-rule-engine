package pl.pajak.dqr.rule.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.engine.Decision;
import pl.pajak.dqr.engine.rule.DecisionMapping;

class DecisionMappingsTest {

  @Test
  void readsTheValueTableAndTheDefaultDecision() {
    final var mappings =
        DecisionMappings.parse(
            yaml(
                """
                decision-mappings:
                  countryBlocked:
                    decision-by-value:
                      blocked: INVALID
                    default-decision: NOT_APPLICABLE
                """));

    final var mapping = mappings.of("countryBlocked");

    assertThat(mapping.decisionOf("blocked"))
        .isEqualTo(new DecisionMapping.Resolution.Matched(Decision.INVALID));
    assertThat(mapping.decisionOf("ok"))
        .isEqualTo(new DecisionMapping.Resolution.Defaulted(Decision.NOT_APPLICABLE));
  }

  @Test
  void leavesValuesUnmappedWhenNoDefaultDecisionIsDeclared() {
    final var mappings =
        DecisionMappings.parse(
            yaml(
                """
                decision-mappings:
                  countryBlocked:
                    decision-by-value:
                      blocked: INVALID
                """));

    assertThat(mappings.of("countryBlocked").decisionOf("ok"))
        .isEqualTo(new DecisionMapping.Resolution.Unmapped("ok"));
  }

  @Test
  void rejectsAnUnknownDecisionName() {
    assertThatThrownBy(
            () ->
                DecisionMappings.parse(
                    yaml(
                        """
                        decision-mappings:
                          countryBlocked:
                            decision-by-value:
                              blocked: BROKEN
                        """)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BROKEN")
        .hasMessageContaining("countryBlocked");
  }

  @Test
  void rejectsAMisspelledKey() {
    assertThatThrownBy(
            () ->
                DecisionMappings.parse(
                    yaml(
                        """
                        decision-mappings:
                          countryBlocked:
                            decision-by-value:
                              blocked: INVALID
                            defaul-decision: NOT_APPLICABLE
                        """)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("defaul-decision");
  }

  @Test
  void rejectsADocumentWithoutTheRootKey() {
    assertThatThrownBy(() -> DecisionMappings.parse(yaml("other-key: {}\n")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("decision-mappings");
  }

  @Test
  void failsWhenARuleHasNoMappingDeclared() {
    final var mappings =
        DecisionMappings.parse(
            yaml(
                """
                decision-mappings:
                  countryBlocked:
                    decision-by-value:
                      blocked: INVALID
                """));

    assertThatThrownBy(() -> mappings.of("somethingElse"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("somethingElse");
  }

  @Test
  void failsWhenTheResourceIsMissing() {
    assertThatThrownBy(() -> DecisionMappings.fromClasspath("no-such-file.yaml"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no-such-file.yaml");
  }

  private static InputStream yaml(String document) {
    return new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8));
  }
}
