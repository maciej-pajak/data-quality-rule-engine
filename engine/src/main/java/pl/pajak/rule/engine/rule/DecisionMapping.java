package pl.pajak.rule.engine.rule;

import pl.pajak.rule.engine.Decision;

public interface DecisionMapping {

  Resolution decisionOf(String value);

  sealed interface Resolution {

    record Matched(Decision decision) implements Resolution {}

    record Defaulted(Decision decision) implements Resolution {}

    record Unmapped(String value) implements Resolution {}
  }
}
