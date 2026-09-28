package pl.pajak.dqr.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.engine.rule.ComputationResult;
import pl.pajak.dqr.engine.rule.CountryScope;
import pl.pajak.dqr.engine.rule.RuleSeverity;
import pl.pajak.dqr.engine.rule.RuleStatus;
import pl.pajak.dqr.engine.testing.FakeRecordSource;
import pl.pajak.dqr.engine.testing.FakeRuleCatalog;
import pl.pajak.dqr.engine.testing.RecordingSink;
import pl.pajak.dqr.engine.testing.TestRecord;
import pl.pajak.dqr.engine.testing.TestRule;

/**
 * Covers what {@link RuleEngine#process(EngineInput)} itself decides: how it reads the source,
 * which rules it selects, which records a rule sees, how a computed value becomes a result, and how
 * a failure is contained. Filter semantics belong to {@link RuleMatcherTest}, counting to {@link
 * RulesRunTest}.
 */
class RuleEngineTest {

  private final RecordingSink sink = new RecordingSink();

  @Nested
  class BatchLoop {

    @Test
    void readsTheSourceInBatchesOfTheRequestedSize() {
      final var source =
          FakeRecordSource.of(
              TestRecord.of("r-1"),
              TestRecord.of("r-2"),
              TestRecord.of("r-3"),
              TestRecord.of("r-4"),
              TestRecord.of("r-5"));

      run(FakeRuleCatalog.of(TestRule.of("rule-1")), source, builder -> builder.withBatchSize(2));

      assertThat(source.requestedSizes()).containsExactly(2, 2, 2, 2);
      assertThat(sink.results())
          .extracting(RuleResult::recordRefId)
          .containsExactly("r-1", "r-2", "r-3", "r-4", "r-5");
    }

    @Test
    void stopsAtTheFirstEmptyBatch() {
      final var source = FakeRecordSource.empty();

      final var summary = run(FakeRuleCatalog.of(TestRule.of("rule-1")), source);

      assertThat(source.readCount()).isEqualTo(1);
      assertThat(sink.results()).isEmpty();
      assertThat(summary.resultsCount()).isZero();
      assertThat(summary.errorsCount()).isZero();
    }

    @Test
    void asksForTheDefaultBatchSizeWhenNoneIsGiven() {
      final var source = FakeRecordSource.of(TestRecord.of("r-1"));

      run(FakeRuleCatalog.of(TestRule.of("rule-1")), source);

      assertThat(source.requestedSizes()).containsOnly(EngineInputImpl.DEFAULT_BATCH_SIZE);
    }
  }

  @Nested
  class RuleSelection {

    @Test
    void evaluatesOnlyReleasedRulesByDefault() {
      final var catalog =
          FakeRuleCatalog.of(
              TestRule.builder("draft").withStatus(RuleStatus.DRAFT).build(),
              TestRule.builder("released").withStatus(RuleStatus.RELEASED).build(),
              TestRule.builder("deprecated").withStatus(RuleStatus.DEPRECATED).build());

      run(catalog, FakeRecordSource.of(TestRecord.of("r-1")));

      assertThat(sink.results()).extracting(RuleResult::ruleId).containsExactly("released");
    }

    @Test
    void appliesAnExplicitFilter() {
      final var catalog =
          FakeRuleCatalog.of(
              TestRule.builder("tax-rule").withCategories("tax").build(),
              TestRule.builder("address-rule").withCategories("address").build());

      run(
          catalog,
          FakeRecordSource.of(TestRecord.of("r-1")),
          builder -> builder.withRuleFilter(RuleFilter.category("tax")));

      assertThat(sink.results()).extracting(RuleResult::ruleId).containsExactly("tax-rule");
    }

    @Test
    void loadsTheCatalogOncePerRunRatherThanPerBatch() {
      final var catalog = FakeRuleCatalog.of(TestRule.of("rule-1"));
      final var source =
          FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2"), TestRecord.of("r-3"));

      run(catalog, source, builder -> builder.withBatchSize(1));

      assertThat(source.readCount()).isEqualTo(4);
      assertThat(catalog.loadCount()).isEqualTo(1);
    }

    @Test
    void producesNothingWhenNoRuleMatchesTheFilter() {
      final var catalog =
          FakeRuleCatalog.of(TestRule.builder("draft").withStatus(RuleStatus.DRAFT).build());

      final var summary = run(catalog, FakeRecordSource.of(TestRecord.of("r-1")));

      assertThat(sink.results()).isEmpty();
      assertThat(sink.errors()).isEmpty();
      assertThat(summary.resultsCount()).isZero();
    }
  }

  @Nested
  class CountryScoping {

    private final FakeRecordSource source =
        FakeRecordSource.of(
            TestRecord.inCountry("pl-record", "PL"),
            TestRecord.inCountry("de-record", "DE"),
            TestRecord.of("stateless-record"));

    @Test
    void aWorldScopedRuleEvaluatesEveryRecord() {
      final var rule = TestRule.builder("rule-1").withCountryScope(CountryScope.World).build();

      run(FakeRuleCatalog.of(rule), source);

      assertThat(sink.results())
          .extracting(RuleResult::recordRefId)
          .containsExactly("pl-record", "de-record", "stateless-record");
    }

    @Test
    void aRuleWithoutAScopeEvaluatesEveryRecord() {
      final var rule = TestRule.builder("rule-1").withoutCountryScope().build();

      run(FakeRuleCatalog.of(rule), source);

      assertThat(sink.results()).hasSize(3);
    }

    @Test
    void aCountryScopedRuleEvaluatesOnlyTheRecordsOfThatCountry() {
      final var rule =
          TestRule.builder("rule-1").withCountryScope(new CountryScope.Country("PL")).build();

      run(FakeRuleCatalog.of(rule), source);

      assertThat(sink.results()).extracting(RuleResult::recordRefId).containsExactly("pl-record");
      assertThat(sink.errors()).isEmpty();
    }

    @Test
    void theCountryIsReadAsAnOrdinaryField() {
      final var rule =
          TestRule.builder("rule-1").withCountryScope(new CountryScope.Country("PL")).build();
      final var records =
          FakeRecordSource.of(
              TestRecord.builder("pl-record").withField("country", "PL").build(),
              TestRecord.builder("de-record").withField("country", "DE").build());

      run(FakeRuleCatalog.of(rule), records);

      assertThat(sink.results()).extracting(RuleResult::recordRefId).containsExactly("pl-record");
    }

    @Test
    void aRecordThatFailsToAnswerItsCountryIsReportedWithoutStoppingTheBatch() {
      final var rule =
          TestRule.builder("rule-1").withCountryScope(new CountryScope.Country("PL")).build();
      final PartnerRecord unreadable =
          new PartnerRecord() {

            @Override
            public String refId() {
              return "unreadable-record";
            }

            @Override
            public String getField(String name) {
              throw new IllegalStateException("record is unreadable");
            }
          };
      final var records = FakeRecordSource.of(unreadable, TestRecord.inCountry("pl-record", "PL"));

      final var summary = run(FakeRuleCatalog.of(rule), records);

      assertThat(sink.results()).extracting(RuleResult::recordRefId).containsExactly("pl-record");
      assertThat(sink.errors())
          .extracting(RuleError::recordRefId, RuleError::message)
          .containsExactly(
              tuple("unreadable-record", "java.lang.IllegalStateException: record is unreadable"));
      assertThat(summary.errorsCount()).isEqualTo(1);
    }

    @Test
    void aWorldScopeCreatedByTheCallerIsStillWorldWide() {
      final var rule =
          TestRule.builder("rule-1").withCountryScope(new CountryScope.World()).build();

      run(FakeRuleCatalog.of(rule), source);

      assertThat(sink.results()).hasSize(3);
    }
  }

  @Nested
  class Results {

    @Test
    void carryTheRuleAndTheRecordThatProducedThem() {
      run(
          FakeRuleCatalog.of(TestRule.builder("rule-1").withLabel("Rule 1").build()),
          FakeRecordSource.of(TestRecord.of("r-1")));

      final var result = sink.results().getFirst();
      assertThat(result.ruleId()).isEqualTo("rule-1");
      assertThat(result.ruleLabel()).isEqualTo("Rule 1");
      assertThat(result.recordRefId()).isEqualTo("r-1");
    }

    @Test
    void carryTheSeverityOfTheRuleAndTheDecisionOfTheMapping() {
      final var rule =
          TestRule.builder("rule-1")
              .withSeverity(RuleSeverity.WARNING)
              .deciding(Decision.REVIEW)
              .build();

      run(FakeRuleCatalog.of(rule), FakeRecordSource.of(TestRecord.of("r-1")));

      final var result = sink.results().getFirst();
      assertThat(result.severity()).isEqualTo(RuleSeverity.WARNING);
      assertThat(result.decision()).isEqualTo(Decision.REVIEW);
    }

    @Test
    void provenanceKeepsTheComputedValueAndHowItWasMapped() {
      final var rule =
          TestRule.builder("rule-1").computing("MISSING").deciding(Decision.INVALID).build();

      run(FakeRuleCatalog.of(rule), FakeRecordSource.of(TestRecord.of("r-1")));

      final var provenance = sink.results().getFirst().provenance();
      assertThat(provenance.computedValue()).isEqualTo("MISSING");
      assertThat(provenance.description()).isEqualTo("the computed value is mapped to INVALID");
    }

    @Test
    void provenanceSaysWhenTheDecisionCameFromTheDefault() {
      final var rule =
          TestRule.builder("rule-1").computing("MISSING").defaultingTo(Decision.REVIEW).build();

      run(FakeRuleCatalog.of(rule), FakeRecordSource.of(TestRecord.of("r-1")));

      assertThat(sink.results().getFirst().provenance().description())
          .isEqualTo("the computed value is not mapped, so the default REVIEW applies");
    }

    @Test
    void provenanceKeepsTheFieldsTheLogicRead() {
      final var rule =
          TestRule.builder("rule-1")
              .withLogic(record -> new ComputationResult.ComputedValue(record.getField("name")))
              .build();
      final var record = TestRecord.builder("r-1").withField("name", "ACME").build();

      run(FakeRuleCatalog.of(rule), new FakeRecordSource(List.of(record)));

      assertThat(sink.results().getFirst().provenance().accessedFields())
          .isEqualTo(Map.of("name", "ACME"));
    }

    @Test
    void everyMatchingRuleIsEvaluatedAgainstEveryRecord() {
      final var catalog = FakeRuleCatalog.of(TestRule.of("rule-1"), TestRule.of("rule-2"));
      final var source = FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2"));

      run(catalog, source);

      assertThat(sink.results())
          .extracting(RuleResult::ruleId, RuleResult::recordRefId)
          .containsExactlyInAnyOrder(
              tuple("rule-1", "r-1"),
              tuple("rule-1", "r-2"),
              tuple("rule-2", "r-1"),
              tuple("rule-2", "r-2"));
    }
  }

  @Nested
  class FailureIsolation {

    @Test
    void aLogicDetectedFailureBecomesAnErrorInsteadOfAResult() {
      final var rule = TestRule.builder("rule-1").failing("no country code").build();

      final var summary = run(FakeRuleCatalog.of(rule), FakeRecordSource.of(TestRecord.of("r-1")));

      assertThat(sink.results()).isEmpty();
      assertThat(sink.errors())
          .extracting(RuleError::ruleId, RuleError::recordRefId, RuleError::message)
          .containsExactly(tuple("rule-1", "r-1", "no country code"));
      assertThat(summary.errorsCount()).isEqualTo(1);
    }

    @Test
    void aThrownExceptionBecomesAnErrorThatKeepsTheCause() {
      final var cause = new IllegalStateException("boom");
      final var rule = TestRule.builder("rule-1").throwing(cause).build();

      run(FakeRuleCatalog.of(rule), FakeRecordSource.of(TestRecord.of("r-1")));

      final var error = sink.errors().getFirst();
      assertThat(error.message()).isEqualTo("java.lang.IllegalStateException: boom");
      assertThat(error.cause()).containsSame(cause);
    }

    @Test
    void anUnmappedComputedValueIsReportedPerRecord() {
      final var rule = TestRule.builder("rule-1").computing("SURPRISE").mappingNothing().build();

      run(FakeRuleCatalog.of(rule), FakeRecordSource.of(TestRecord.of("r-1")));

      assertThat(sink.results()).isEmpty();
      assertThat(sink.errors())
          .extracting(RuleError::message)
          .containsExactly(
              "computed value \"SURPRISE\" is not mapped and the rule declares no default decision");
    }

    @Test
    void aThrowingDecisionMappingIsContainedLikeAnyOtherFailure() {
      final var rule =
          TestRule.builder("rule-1")
              .withDecisionMapping(
                  value -> {
                    throw new IllegalStateException("mapping unavailable");
                  })
              .build();

      run(FakeRuleCatalog.of(rule), FakeRecordSource.of(TestRecord.of("r-1")));

      assertThat(sink.results()).isEmpty();
      assertThat(sink.errors()).hasSize(1);
    }

    @Test
    void theRemainingRecordsAreStillEvaluated() {
      final var rule =
          TestRule.builder("rule-1")
              .withLogic(
                  record -> {
                    if ("r-2".equals(record.refId())) {
                      throw new IllegalStateException("boom");
                    }
                    return new ComputationResult.ComputedValue("value");
                  })
              .build();
      final var source =
          FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2"), TestRecord.of("r-3"));

      run(FakeRuleCatalog.of(rule), source);

      assertThat(sink.results()).extracting(RuleResult::recordRefId).containsExactly("r-1", "r-3");
      assertThat(sink.errors()).extracting(RuleError::recordRefId).containsExactly("r-2");
    }

    /**
     * A sink that refuses ends the run instead of being asked again for every record left. It is
     * the one failure the engine does not isolate: a rule is one of many, the sink is the only
     * destination there is, and the error reporting the loss would go to the same sink.
     */
    @Test
    void aSinkThatRefusesAResultEndsTheRunInsteadOfBeingAskedAgain() {
      final var refusing = new RefusingSink(true, false);
      final var source =
          FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2"), TestRecord.of("r-3"));

      assertThatThrownBy(() -> runInto(FakeRuleCatalog.of(TestRule.of("rule-1")), source, refusing))
          .isInstanceOf(ResultSinkException.class)
          .hasCauseInstanceOf(IllegalStateException.class)
          .satisfies(
              thrown -> {
                final var sinkFailure = (ResultSinkException) thrown;
                assertThat(sinkFailure.recordRefId()).isEqualTo("r-1");
                assertThat(sinkFailure.ruleId()).isEqualTo("rule-1");
              });

      assertThat(refusing.calls()).as("the sink is not handed the records that were left").isOne();
    }

    /** The same holds for the other half of the sink: an error it refuses ends the run too. */
    @Test
    void aSinkThatRefusesAnErrorEndsTheRun() {
      final var refusing = new RefusingSink(false, true);
      final var failing = TestRule.builder("rule-1").throwing(new IllegalStateException("no"));

      assertThatThrownBy(
              () ->
                  runInto(
                      FakeRuleCatalog.of(failing.build()),
                      FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2")),
                      refusing))
          .isInstanceOf(ResultSinkException.class)
          .hasMessageContaining("an error")
          .hasMessageContaining("r-1");

      assertThat(refusing.calls()).isOne();
    }

    /** A run the sink ended reports no summary; what it accepted before the failure stands. */
    @Test
    void aRunTheSinkEndedReportsNoSummary() {
      final var refusing = new RefusingSink(true, false);
      refusing.acceptFirst(2);

      assertThatThrownBy(
              () ->
                  runInto(
                      FakeRuleCatalog.of(TestRule.of("rule-1")),
                      FakeRecordSource.of(
                          TestRecord.of("r-1"), TestRecord.of("r-2"), TestRecord.of("r-3")),
                      refusing))
          .isInstanceOf(ResultSinkException.class);

      assertThat(refusing.accepted())
          .extracting(RuleResult::recordRefId)
          .containsExactly("r-1", "r-2");
    }

    @Test
    void theOtherRulesAreStillEvaluated() {
      final var catalog =
          FakeRuleCatalog.of(
              TestRule.builder("broken").throwing(new IllegalStateException("boom")).build(),
              TestRule.of("healthy"));
      final var source = FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2"));

      final var summary = run(catalog, source);

      assertThat(sink.results()).extracting(RuleResult::ruleId).containsOnly("healthy");
      assertThat(summary.resultsCount()).isEqualTo(2);
      assertThat(summary.errorsCount()).isEqualTo(2);
    }
  }

  @Nested
  class Streaming {

    @Test
    void resultsReachTheSinkBeforeTheSourceIsDrained() {
      final var source =
          FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2"), TestRecord.of("r-3"));
      final var remainingAtEachResult = new ArrayList<Integer>();
      final RuleResultSink observingSink =
          new RecordingSink() {

            @Override
            public void onResult(RuleResult result) {
              remainingAtEachResult.add(source.remainingRecords());
              super.onResult(result);
            }
          };

      RuleEngine.newInstance()
          .process(
              EngineInput.builder(FakeRuleCatalog.of(TestRule.of("rule-1")), source, observingSink)
                  .withBatchSize(1)
                  .build());

      assertThat(remainingAtEachResult).containsExactly(2, 1, 0);
    }
  }

  @Nested
  class Summary {

    @Test
    void countsExactlyWhatTheSinkReceived() {
      final var catalog =
          FakeRuleCatalog.of(
              TestRule.builder("valid-rule").deciding(Decision.VALID).build(),
              TestRule.builder("review-rule")
                  .withSeverity(RuleSeverity.WARNING)
                  .deciding(Decision.REVIEW)
                  .build(),
              TestRule.builder("failing-rule").failing("no country code").build());
      final var source = FakeRecordSource.of(TestRecord.of("r-1"), TestRecord.of("r-2"));

      final var summary = run(catalog, source);

      assertThat(summary.resultsCount()).isEqualTo(sink.results().size()).isEqualTo(4);
      assertThat(summary.errorsCount()).isEqualTo(sink.errors().size()).isEqualTo(2);
      assertThat(summary.resultsCountByDecision())
          .isEqualTo(Map.of(Decision.VALID, 2L, Decision.REVIEW, 2L));
      assertThat(summary.resultsCountBySeverity())
          .isEqualTo(Map.of(RuleSeverity.ERROR, 2L, RuleSeverity.WARNING, 2L));
    }
  }

  private RunSummary run(FakeRuleCatalog catalog, FakeRecordSource source) {
    return run(catalog, source, UnaryOperator.identity());
  }

  private RunSummary run(
      FakeRuleCatalog catalog,
      FakeRecordSource source,
      UnaryOperator<EngineInput.Builder> configure) {
    final var builder = configure.apply(EngineInput.builder(catalog, source, sink));
    return RuleEngine.newInstance().process(builder.build());
  }

  private static RunSummary runInto(
      FakeRuleCatalog catalog, FakeRecordSource source, RuleResultSink into) {
    return RuleEngine.newInstance().process(EngineInput.builder(catalog, source, into).build());
  }

  /**
   * A sink that refuses what it is handed, after accepting a given number of results first. It
   * counts every call, so a test can assert that the engine stopped asking.
   */
  private static final class RefusingSink implements RuleResultSink {

    private final boolean refuseResults;
    private final boolean refuseErrors;
    private final List<RuleResult> accepted = new ArrayList<>();

    private int calls = 0;
    private int acceptFirst = 0;

    private RefusingSink(boolean refuseResults, boolean refuseErrors) {
      this.refuseResults = refuseResults;
      this.refuseErrors = refuseErrors;
    }

    /** Accepts {@code count} results before it starts refusing. */
    void acceptFirst(int count) {
      this.acceptFirst = count;
    }

    @Override
    public void onResult(RuleResult result) {
      calls++;
      if (refuseResults && accepted.size() >= acceptFirst) {
        throw new IllegalStateException("the response is already committed");
      }
      accepted.add(result);
    }

    @Override
    public void onError(RuleError error) {
      calls++;
      if (refuseErrors) {
        throw new IllegalStateException("the response is already committed");
      }
    }

    /** How often either method was called, refusals included. */
    int calls() {
      return calls;
    }

    List<RuleResult> accepted() {
      return List.copyOf(accepted);
    }
  }
}
