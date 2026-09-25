package pl.pajak.rule.engine;

import java.util.Map;
import pl.pajak.rule.engine.rule.RuleSeverity;

record RuleResultImpl(
    String recordRefIf,
    String ruleId,
    Decision decision,
    RuleSeverity severity,
    Provenance provenance)
    implements RuleResult {

  record ProvenanceImpl(
      String computedValue, String description, Map<String, String> accessedFields)
      implements Provenance {}
}
