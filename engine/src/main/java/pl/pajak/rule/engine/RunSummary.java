package pl.pajak.rule.engine;

import java.util.Map;
import javax.print.attribute.standard.Severity;

public interface RunSummary {

  int resultsCount();

  int errorsCount();

  Map<Decision, Integer> recordsCountByDecision();

  Map<Severity, Integer> recordsCountBySeverity();
}
