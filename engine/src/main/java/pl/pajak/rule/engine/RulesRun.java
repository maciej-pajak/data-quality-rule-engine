package pl.pajak.rule.engine;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import pl.pajak.rule.engine.rule.ComputationResult;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleSeverity;

/**
 * The state of one run: it forwards every result and every error to the sink as they are produced,
 * and keeps only the counters needed for the summary.
 *
 * <p>Not thread safe; a run evaluates rules on the calling thread.
 */
class RulesRun {

  private final RuleResultSink resultSink;
  private final Map<Decision, Integer> resultsCountByDecision = new EnumMap<>(Decision.class);
  private final Map<RuleSeverity, Integer> resultsCountBySeverity =
      new EnumMap<>(RuleSeverity.class);
  private int resultsCount = 0;
  private int errorsCount = 0;

  RulesRun(RuleResultSink resultSink) {
    this.resultSink = Objects.requireNonNull(resultSink, "resultSink cannot be null");
  }

  RunSummary getSummary() {
    return new RunSummaryImpl(
        resultsCount, errorsCount, resultsCountByDecision, resultsCountBySeverity);
  }

  void onResult(RuleResult result) {
    countResult(result);
    resultSink.onSuccess(result);
  }

  /** Reports a failure the rule detected itself. */
  void onFailure(Rule rule, PartnerRecord record, ComputationResult.ComputationError error) {
    onFailure(new RuleErrorImpl(record.refId(), rule.id(), error.message(), null));
  }

  /** Reports a failure a rule threw out of. */
  void onFailure(Rule rule, PartnerRecord record, Throwable cause) {
    onFailure(new RuleErrorImpl(record.refId(), rule.id(), describe(cause), cause));
  }

  private void onFailure(RuleError error) {
    errorsCount++;
    resultSink.onError(error);
  }

  private void countResult(RuleResult result) {
    resultsCount++;
    resultsCountByDecision.merge(result.decision(), 1, Integer::sum);
    resultsCountBySeverity.merge(result.severity(), 1, Integer::sum);
  }

  private static String describe(Throwable cause) {
    final var message = cause.getMessage();
    return message == null || message.isBlank()
        ? cause.getClass().getName()
        : cause.getClass().getName() + ": " + message;
  }
}
