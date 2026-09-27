package pl.pajak.rule.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.pajak.rule.engine.rule.ComputationResult;
import pl.pajak.rule.engine.rule.CountryScope;
import pl.pajak.rule.engine.rule.DecisionMapping;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleSeverity;
import pl.pajak.rule.engine.rule.RuleStatus;
import pl.pajak.rule.engine.rule.ValueLogic;

class RulesRunTest {

  private final RecordingSink sink = new RecordingSink();
  private final RulesRun run = new RulesRun(sink);

  @Test
  void forwardsResultsAsTheyAreProduced() {
    run.onResult(result("r-1", Decision.VALID, RuleSeverity.ERROR));
    run.onResult(result("r-2", Decision.INVALID, RuleSeverity.WARNING));

    assertThat(sink.results).extracting(RuleResult::recordRefId).containsExactly("r-1", "r-2");
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
        rule("rule-1"), record("r-1"), new ComputationResult.ComputationError("no country code"));
    run.onResult(result("r-2", Decision.VALID, RuleSeverity.INFO));

    final var summary = run.getSummary();

    assertThat(summary.errorsCount()).isEqualTo(1);
    assertThat(summary.resultsCount()).isEqualTo(1);
    assertThat(summary.resultsCountByDecision()).isEqualTo(Map.of(Decision.VALID, 1));
  }

  @Test
  void reportedFailureLocatesRuleAndRecord() {
    run.onFailure(
        rule("rule-1"), record("r-1"), new ComputationResult.ComputationError("no country code"));

    assertThat(sink.errors).hasSize(1);
    final var error = sink.errors.getFirst();
    assertThat(error.ruleId()).isEqualTo("rule-1");
    assertThat(error.recordRefId()).isEqualTo("r-1");
    assertThat(error.message()).isEqualTo("no country code");
    assertThat(error.cause()).isEmpty();
  }

  @Test
  void reportedFailureKeepsTheException() {
    final var cause = new IllegalStateException("boom");

    run.onFailure(rule("rule-1"), record("r-1"), cause);

    final var error = sink.errors.getFirst();
    assertThat(error.message()).isEqualTo("java.lang.IllegalStateException: boom");
    assertThat(error.cause()).containsSame(cause);
  }

  @Test
  void namesTheExceptionTypeWhenItCarriesNoMessage() {
    run.onFailure(rule("rule-1"), record("r-1"), new NullPointerException());

    assertThat(sink.errors.getFirst().message()).isEqualTo("java.lang.NullPointerException");
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

  private static PartnerRecord record(String refId) {
    return new PartnerRecord() {

      @Override
      public String refId() {
        return refId;
      }

      @Override
      public Optional<CountryScope> getCountry() {
        return Optional.empty();
      }

      @Override
      public void read(PartnerRecordVisitor visitor) {}

      @Override
      public String getField(String name) {
        return null;
      }
    };
  }

  private static Rule rule(String id) {
    return new Rule() {

      @Override
      public String id() {
        return id;
      }

      @Override
      public String label() {
        return id;
      }

      @Override
      public RuleStatus status() {
        return RuleStatus.RELEASED;
      }

      @Override
      public RuleSeverity severity() {
        return RuleSeverity.ERROR;
      }

      @Override
      public List<String> categories() {
        return List.of();
      }

      @Override
      public Optional<CountryScope> countryScope() {
        return Optional.empty();
      }

      @Override
      public ValueLogic logic() {
        return partnerRecord -> new ComputationResult.ComputedValue("value");
      }

      @Override
      public DecisionMapping decisionMapping() {
        return value -> new DecisionMapping.Resolution.Defaulted(Decision.VALID);
      }
    };
  }

  private static final class RecordingSink implements RuleResultSink {

    private final List<RuleResult> results = new ArrayList<>();
    private final List<RuleError> errors = new ArrayList<>();

    @Override
    public void onSuccess(RuleResult result) {
      results.add(result);
    }

    @Override
    public void onError(RuleError error) {
      errors.add(error);
    }
  }
}
