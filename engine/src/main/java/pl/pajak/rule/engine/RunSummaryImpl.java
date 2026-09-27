package pl.pajak.rule.engine;

import java.util.Map;
import pl.pajak.rule.engine.rule.RuleSeverity;

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
