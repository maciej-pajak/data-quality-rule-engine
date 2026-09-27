package pl.pajak.dqr.engine;

import java.util.Map;
import pl.pajak.dqr.engine.rule.RuleSeverity;

record RunSummaryImpl(
    int resultsCount,
    int errorsCount,
    Map<Decision, Integer> resultsCountByDecision,
    Map<RuleSeverity, Integer> resultsCountBySeverity)
    implements RunSummary {

  RunSummaryImpl {
    resultsCountByDecision = Map.copyOf(resultsCountByDecision);
    resultsCountBySeverity = Map.copyOf(resultsCountBySeverity);
  }
}
