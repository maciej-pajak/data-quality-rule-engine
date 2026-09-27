package pl.pajak.dqr.example.rules;

import java.util.List;
import java.util.Optional;
import pl.pajak.dqr.engine.rule.CountryScope;
import pl.pajak.dqr.engine.rule.DecisionMapping;
import pl.pajak.dqr.engine.rule.Rule;
import pl.pajak.dqr.engine.rule.RuleSeverity;
import pl.pajak.dqr.engine.rule.RuleStatus;
import pl.pajak.dqr.engine.rule.ValueLogic;

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
