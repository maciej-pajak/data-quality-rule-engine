package pl.pajak.rule.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import pl.pajak.rule.engine.rule.ComputationResult;
import pl.pajak.rule.engine.rule.RuleSeverity;
import pl.pajak.rule.engine.testing.RecordingSink;
import pl.pajak.rule.engine.testing.TestRecord;
import pl.pajak.rule.engine.testing.TestRule;

class RulesRunTest {

  private final RecordingSink sink = new RecordingSink();
  private final RulesRun run = new RulesRun(sink);

  @Test
  void forwardsResultsAsTheyAreProduced() {
    run.onResult(result("r-1", Decision.VALID, RuleSeverity.ERROR));
    run.onResult(result("r-2", Decision.INVALID, RuleSeverity.WARNING));

    assertThat(sink.results()).extracting(RuleResult::recordRefId).containsExactly("r-1", "r-2");
  }

  @Test
  void countsResultsPerDecisionAndSeverity() {
    run.onResult(result("r-1", Decision.VALID, RuleSeverity.ERROR));
    run.onResult(result("r-2", Decision.VALID, RuleSeverity.WARNING));
    run.onResult(result("r-3", Decision.REVIEW, RuleSeverity.ERROR));

    final var summary = run.getSummary();

    assertThat(summary.resultsCount()).isEqualTo(3);
    assertThat(summary.errorsCount()).isZero();
    assertThat(summary.resultsCountByDecision())
        .isEqualTo(Map.of(Decision.VALID, 2, Decision.REVIEW, 1));
    assertThat(summary.resultsCountBySeverity())
        .isEqualTo(Map.of(RuleSeverity.ERROR, 2, RuleSeverity.WARNING, 1));
  }

  @Test
  void keepsRunningAfterAFailingRule() {
    run.onFailure(
        TestRule.of("rule-1"),
        TestRecord.of("r-1"),
        new ComputationResult.ComputationError("no country code"));
    run.onResult(result("r-2", Decision.VALID, RuleSeverity.INFO));

    final var summary = run.getSummary();

    assertThat(summary.errorsCount()).isEqualTo(1);
    assertThat(summary.resultsCount()).isEqualTo(1);
    assertThat(summary.resultsCountByDecision()).isEqualTo(Map.of(Decision.VALID, 1));
  }

  @Test
  void reportedFailureLocatesRuleAndRecord() {
    run.onFailure(
        TestRule.of("rule-1"),
        TestRecord.of("r-1"),
        new ComputationResult.ComputationError("no country code"));

    assertThat(sink.errors()).hasSize(1);
    final var error = sink.errors().getFirst();
    assertThat(error.ruleId()).isEqualTo("rule-1");
    assertThat(error.recordRefId()).isEqualTo("r-1");
    assertThat(error.message()).isEqualTo("no country code");
    assertThat(error.cause()).isEmpty();
  }

  @Test
  void reportedFailureKeepsTheException() {
    final var cause = new IllegalStateException("boom");

    run.onFailure(TestRule.of("rule-1"), TestRecord.of("r-1"), cause);

    final var error = sink.errors().getFirst();
    assertThat(error.message()).isEqualTo("java.lang.IllegalStateException: boom");
    assertThat(error.cause()).containsSame(cause);
  }

  @Test
  void namesTheExceptionTypeWhenItCarriesNoMessage() {
    run.onFailure(TestRule.of("rule-1"), TestRecord.of("r-1"), new NullPointerException());

    assertThat(sink.errors().getFirst().message()).isEqualTo("java.lang.NullPointerException");
  }

  @Test
  void summaryIsASnapshotOfTheRunSoFar() {
    run.onResult(result("r-1", Decision.VALID, RuleSeverity.ERROR));
    final var summary = run.getSummary();

    run.onResult(result("r-2", Decision.VALID, RuleSeverity.ERROR));

    assertThat(summary.resultsCount()).isEqualTo(1);
    assertThat(summary.resultsCountByDecision()).isEqualTo(Map.of(Decision.VALID, 1));
  }

  private static RuleResult result(String recordRefId, Decision decision, RuleSeverity severity) {
    return new RuleResultImpl(
        recordRefId,
        "rule-1",
        decision,
        severity,
        new RuleResultImpl.ProvenanceImpl("value", "description", Map.of()));
  }
}
