package pl.pajak.dqr.engine;

import java.util.Map;
import pl.pajak.dqr.engine.rule.RuleSeverity;

record RuleResultImpl(
    String recordRefId,
    String ruleId,
    Decision decision,
    RuleSeverity severity,
    Provenance provenance)
    implements RuleResult {

  record ProvenanceImpl(
      String computedValue, String description, Map<String, String> accessedFields)
      implements Provenance {}
}
