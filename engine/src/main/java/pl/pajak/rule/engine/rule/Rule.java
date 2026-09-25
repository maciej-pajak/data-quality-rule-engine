package pl.pajak.rule.engine.rule;

import java.util.List;
import java.util.Optional;

public interface Rule {

  String id();

  String label();

  RuleStatus status();

  RuleSeverity severity();

  List<String> categories();

  Optional<CountryScope> countryScope();

  ValueLogic logic();

  DecisionMapping decisionMapping();
}
