package pl.pajak.dqr.rule.java;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.rule.java.testing.JsonPartnerRecords;
import pl.pajak.dqr.rule.java.testing.ListRecordSource;
import pl.pajak.dqr.rule.java.testing.RecordingSink;
import pl.pajak.rule.engine.Decision;
import pl.pajak.rule.engine.EngineInput;
import pl.pajak.rule.engine.RuleEngine;
import pl.pajak.rule.engine.RuleError;
import pl.pajak.rule.engine.RuleResult;
import pl.pajak.rule.engine.rule.ComputationResult;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleCatalog;
import pl.pajak.rule.engine.rule.RuleSeverity;
import pl.pajak.rule.engine.rule.RuleStatus;

/**
 * The scenario from the task description, run end to end in one pass: the real engine over the real
 * catalog and the three fixture records, with only the source and the sink replaced.
 *
 * <p>The catalog is extended with one rule that throws on {@code r2}, so the same run also shows
 * fault isolation: that failure is reported for that record alone and every other evaluation in the
 * batch still produces its decision.
 */
class FixtureScenarioTest {

  private static final String RECORDS_RESOURCE = "fixtures/records.json";

  private static final String FAILING_RULE_ID = "vatChecksum";

  private static final String FAILING_RECORD_REF_ID = "r2";

  @Test
  void decidesTheFixtureBatchAndIsolatesTheFailingRule() {
    final var records = JsonPartnerRecords.fromClasspath(RECORDS_RESOURCE);
    final var sink = new RecordingSink();

    final var summary =
        new RuleEngine()
            .process(
                EngineInput.builder(catalogWithFailingRule(), new ListRecordSource(records), sink)
                    .build());

    assertThat(sink.results())
        .extracting(
            RuleResult::recordRefId,
            RuleResult::ruleId,
            RuleResult::decision,
            result -> result.provenance().computedValue())
        .containsExactlyInAnyOrder(
            // The expected results of the task description, record by record, each with the value
            // its rule computed.
            tuple("r1", "countryBlocked", Decision.NOT_APPLICABLE, "ok"),
            tuple("r1", "vatFormat", Decision.VALID, "ok"),
            tuple("r1", "ibanFormat", Decision.VALID, "ok"),
            tuple("r2", "countryBlocked", Decision.NOT_APPLICABLE, "ok"),
            tuple("r2", "vatFormat", Decision.INVALID, "bad"),
            tuple("r2", "ibanFormat", Decision.VALID, "ok"),
            tuple("r3", "countryBlocked", Decision.INVALID, "blocked"),
            tuple("r3", "vatFormat", Decision.INVALID, "bad"),
            tuple("r3", "ibanFormat", Decision.INVALID, "bad"),
            // The failing rule fails on r2 only, so r1 and r3 are still decided.
            tuple("r1", FAILING_RULE_ID, Decision.VALID, "ok"),
            tuple("r3", FAILING_RULE_ID, Decision.VALID, "ok"));

    // The reasons the task's table names in parentheses: the field read that produced the decision.
    assertThat(fieldsReadBy("r2", "vatFormat", sink))
        .as("r2 is INVALID because its vatId is 4 characters long")
        .containsEntry("vatId", "FR22");
    assertThat(fieldsReadBy("r3", "vatFormat", sink))
        .as("r3 is INVALID because it carries no vatId at all")
        .containsEntry("vatId", null);
    assertThat(fieldsReadBy("r3", "ibanFormat", sink))
        .as("r3 is INVALID because its iban is empty")
        .containsEntry("iban", "");

    assertThat(sink.errors())
        .extracting(RuleError::recordRefId, RuleError::ruleId)
        .containsExactly(tuple(FAILING_RECORD_REF_ID, FAILING_RULE_ID));
    assertThat(sink.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.message()).contains("no checksum service reachable");
              assertThat(error.cause()).containsInstanceOf(IllegalStateException.class);
            });
    assertThat(summary.resultsCount()).isEqualTo(11);
    assertThat(summary.errorsCount()).isOne();
    assertThat(summary.resultsCountByDecision())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(Decision.VALID, 5, Decision.INVALID, 4, Decision.NOT_APPLICABLE, 2));
  }

  /** The fields one rule read while it decided one record, as the result's provenance kept them. */
  private static Map<String, String> fieldsReadBy(
      String recordRefId, String ruleId, RecordingSink sink) {
    return sink.results().stream()
        .filter(it -> it.recordRefId().equals(recordRefId) && it.ruleId().equals(ruleId))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No result for " + recordRefId + "/" + ruleId))
        .provenance()
        .accessedFields();
  }

  /** The catalog from the task description plus one rule that throws on a single record. */
  private static RuleCatalog catalogWithFailingRule() {
    final var rules = new ArrayList<Rule>(new JavaRuleCatalog().load());
    rules.add(failingRule());
    final var catalog = List.copyOf(rules);
    return () -> catalog;
  }

  private static Rule failingRule() {
    return new JavaRule(
        FAILING_RULE_ID,
        "Verifies the VAT id against an external checksum service.",
        RuleStatus.RELEASED,
        RuleSeverity.ERROR,
        List.of(),
        Optional.empty(),
        record -> {
          if (record.refId().equals(FAILING_RECORD_REF_ID)) {
            throw new IllegalStateException("no checksum service reachable");
          }
          return new ComputationResult.ComputedValue("ok");
        },
        new ValueDecisionMapping(Map.of("ok", Decision.VALID), Optional.empty()));
  }
}
