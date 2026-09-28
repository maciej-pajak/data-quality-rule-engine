package pl.pajak.dqr.engine;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import pl.pajak.dqr.engine.rule.ComputationResult;
import pl.pajak.dqr.engine.rule.Rule;
import pl.pajak.dqr.engine.rule.RuleSeverity;

/**
 * The state of one run: it forwards every result and every error to the sink as they are produced,
 * and keeps only the counters needed for the summary. A failure of the sink is not counted as one
 * of the run's errors; it ends the run as a {@link ResultSinkException}.
 *
 * <p>Not thread safe; a run evaluates rules on the calling thread.
 */
final class RulesRun {

  private final RuleResultSink resultSink;
  private final Map<Decision, Long> resultsCountByDecision = new EnumMap<>(Decision.class);
  private final Map<RuleSeverity, Long> resultsCountBySeverity = new EnumMap<>(RuleSeverity.class);
  private long resultsCount = 0;
  private long errorsCount = 0;

  RulesRun(RuleResultSink resultSink) {
    this.resultSink = Objects.requireNonNull(resultSink, "resultSink cannot be null");
  }

  RunSummary getSummary() {
    return new RunSummaryImpl(
        resultsCount, errorsCount, resultsCountByDecision, resultsCountBySeverity);
  }

  /** Hands a result to the sink and counts it, so that only what the sink accepted is counted. */
  void onResult(RuleResult result) {
    try {
      resultSink.onResult(result);
    } catch (Exception ex) {
      throw new ResultSinkException(result.recordRefId(), result.ruleId(), "a result", ex);
    }
    countResult(result);
  }

  /** Reports a failure the rule detected itself. */
  void onFailure(Rule rule, PartnerRecord record, ComputationResult.ComputationError error) {
    onFailure(rule, record, error.message());
  }

  /** Reports a failure the engine detected while turning a computed value into a decision. */
  void onFailure(Rule rule, PartnerRecord record, String message) {
    onFailure(new RuleErrorImpl(record.refId(), rule.id(), rule.label(), message, null));
  }

  /** Reports a failure a rule threw out of. */
  void onFailure(Rule rule, PartnerRecord record, Throwable cause) {
    onFailure(new RuleErrorImpl(record.refId(), rule.id(), rule.label(), describe(cause), cause));
  }

  private void onFailure(RuleError error) {
    try {
      resultSink.onError(error);
    } catch (Exception ex) {
      throw new ResultSinkException(error.recordRefId(), error.ruleId(), "an error", ex);
    }
    errorsCount++;
  }

  private void countResult(RuleResult result) {
    resultsCount++;
    resultsCountByDecision.merge(result.decision(), 1L, Long::sum);
    resultsCountBySeverity.merge(result.severity(), 1L, Long::sum);
  }

  private static String describe(Throwable cause) {
    final var message = cause.getMessage();
    return message == null || message.isBlank()
        ? cause.getClass().getName()
        : cause.getClass().getName() + ": " + message;
  }
}
