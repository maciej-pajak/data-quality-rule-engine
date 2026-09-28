package pl.pajak.dqr.engine;

import java.util.Map;
import pl.pajak.dqr.engine.rule.RuleSeverity;

/**
 * What a finished run produced, returned by {@link RuleEngine#process(EngineInput)}.
 *
 * <p>The counters are per result, not per record: one record evaluated by three rules contributes
 * three results. The number of distinct records is not reported, as a run over an unbounded source
 * cannot keep every identifier it has seen.
 *
 * <p>The summary carries counts only; the outcomes themselves reach the caller through its {@link
 * RuleResultSink} while the run proceeds. Instances are immutable.
 */
public sealed interface RunSummary permits RunSummaryImpl {

  /**
   * Returns how many results the run produced.
   *
   * @return the number of calls to {@link RuleResultSink#onResult(RuleResult)} the sink accepted
   */
  long resultsCount();

  /**
   * Returns how many rule failures the run produced.
   *
   * @return the number of calls made to {@link RuleResultSink#onError(RuleError)}
   */
  long errorsCount();

  /**
   * Returns the result counts broken down by decision.
   *
   * @return an unmodifiable map of decision to count; a decision that never occurred is absent
   */
  Map<Decision, Long> resultsCountByDecision();

  /**
   * Returns the result counts broken down by the severity of the rule that produced them.
   *
   * @return an unmodifiable map of severity to count; a severity that never occurred is absent
   */
  Map<RuleSeverity, Long> resultsCountBySeverity();
}
