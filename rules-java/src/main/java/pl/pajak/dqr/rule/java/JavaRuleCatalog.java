package pl.pajak.dqr.rule.java;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import pl.pajak.rule.engine.rule.ComputationResult;
import pl.pajak.rule.engine.rule.DecisionMapping;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleCatalog;
import pl.pajak.rule.engine.rule.RuleSeverity;
import pl.pajak.rule.engine.rule.RuleStatus;

public class JavaRuleCatalog implements RuleCatalog {

  @Override
  public List<Rule> load() {
    return List.of(
        new JavaRule(
            "countryBlocked",
            "Is country blocked.",
            RuleStatus.RELEASED,
            RuleSeverity.WARNING,
            List.of(),
            Optional.empty(),
            record -> {
              String fieldValue = record.getField("country");
              String value = Objects.equals("ZZ", fieldValue) ? "blocked" : "ok";
              return new ComputationResult.ComputedValue(value);
            },
            loadDecisionMapping("countryBlocked")));
  }

  private DecisionMapping loadDecisionMapping(String ruleId) {
    return null;
  }
}
