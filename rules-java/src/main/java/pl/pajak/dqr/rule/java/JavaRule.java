package pl.pajak.dqr.rule.java;

import java.util.List;
import java.util.Optional;
import pl.pajak.rule.engine.rule.CountryScope;
import pl.pajak.rule.engine.rule.DecisionMapping;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleSeverity;
import pl.pajak.rule.engine.rule.RuleStatus;
import pl.pajak.rule.engine.rule.ValueLogic;

public record JavaRule(
    String id,
    String label,
    RuleStatus status,
    RuleSeverity severity,
    List<String> categories,
    Optional<CountryScope> countryScope,
    ValueLogic logic,
    DecisionMapping decisionMapping)
    implements Rule {}
