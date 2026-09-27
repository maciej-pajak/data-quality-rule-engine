package pl.pajak.rule.engine;

import java.util.Map;
import pl.pajak.rule.engine.rule.RuleSeverity;

/**
 * An immutable snapshot of what a finished run produced.
 *
 * <p>The counters are per result, not per record: one record evaluated by three rules contributes
 * three results. Counting distinct records would require remembering every reference seen, which a
 * run over an unbounded source cannot afford.
 */
public sealed interface RunSummary permits RunSummaryImpl {

  /** Number of results handed to {@link RuleResultSink#onSuccess(RuleResult)}. */
  int resultsCount();

  /** Number of errors handed to {@link RuleResultSink#onError(RuleError)}. */
  int errorsCount();

  /** Result counts per decision. Decisions that never occurred are absent. */
  Map<Decision, Integer> resultsCountByDecision();

  /** Result counts per severity of the rule that produced them. Unused severities are absent. */
  Map<RuleSeverity, Integer> resultsCountBySeverity();
}
