package pl.pajak.rule.engine;

import pl.pajak.rule.engine.rule.Rule;

class RuleMatcher {

  static boolean matches(RuleFilter filter, Rule rule) {
    return switch (filter) {
      case RuleFilter.All all -> true;
      case RuleFilter.Category category -> rule.categories().contains(category.category());
      case RuleFilter.Status status -> rule.status() == status.status();
      case RuleFilter.And and -> and.filters().stream().allMatch(f -> matches(f, rule));
      case RuleFilter.Not not -> !matches(not.filter(), rule);
      case RuleFilter.Or or -> or.filters().stream().anyMatch(f -> matches(f, rule));
    };
  }
}
