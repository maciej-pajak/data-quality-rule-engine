package pl.pajak.dqr.engine;

import java.util.Map;
import pl.pajak.dqr.engine.rule.RuleSeverity;

record RunSummaryImpl(
    long resultsCount,
    long errorsCount,
    Map<Decision, Long> resultsCountByDecision,
    Map<RuleSeverity, Long> resultsCountBySeverity)
    implements RunSummary {

  RunSummaryImpl {
    resultsCountByDecision = Map.copyOf(resultsCountByDecision);
    resultsCountBySeverity = Map.copyOf(resultsCountBySeverity);
  }
}
