package pl.pajak.dqr.engine;

import java.util.Map;
import pl.pajak.dqr.engine.rule.RuleSeverity;

public sealed interface RuleResult permits RuleResultImpl {

  String recordRefId();

  String ruleId();

  Decision decision();

  RuleSeverity severity();

  Provenance provenance();

  interface Provenance {

    String computedValue();

    String description();

    Map<String, String> accessedFields();
  }
}
