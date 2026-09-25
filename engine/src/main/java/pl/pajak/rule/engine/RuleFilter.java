package pl.pajak.rule.engine;

import java.util.Arrays;
import java.util.List;
import pl.pajak.rule.engine.rule.RuleStatus;

public sealed interface RuleFilter {

  static List<RuleFilter> of(RuleFilter... filters) {
    return Arrays.stream(filters).toList();
  }

  public static RuleFilter all() {
    return RuleFilter.All.INSTANCE;
  }

  public static RuleFilter status(RuleStatus status) {
    return new RuleFilter.Status(status);
  }

  public static RuleFilter category(String category) {
    return new RuleFilter.Category(category);
  }

  public static RuleFilter countryScope(String country) {
    return new RuleFilter.CountryScope(country);
  }

  public static RuleFilter and(RuleFilter... filters) {
    return new RuleFilter.And(RuleFilter.of(filters));
  }

  public static RuleFilter or(RuleFilter... filters) {
    return new RuleFilter.Or(RuleFilter.of(filters));
  }

  public static RuleFilter not(RuleFilter filter) {
    return new RuleFilter.Not(filter);
  }

  default RuleFilter and(RuleFilter other) {
    return new And(List.of(this, other));
  }

  default RuleFilter or(RuleFilter other) {
    return new Or(List.of(this, other));
  }

  default RuleFilter negate() {
    return new Not(this);
  }

  final class All implements RuleFilter {

    public static final RuleFilter.All INSTANCE = new RuleFilter.All();

    private All() {}
  }

  record Status(RuleStatus status) implements RuleFilter {}

  record Category(String category) implements RuleFilter {}

  record CountryScope(String country) implements RuleFilter {}

  record And(List<RuleFilter> filters) implements RuleFilter {

    public And {
      if (filters.size() < 2) {
        throw new IllegalArgumentException(
            "And requires at least 2 filters, got " + filters.size());
      }
    }
  }

  record Or(List<RuleFilter> filters) implements RuleFilter {

    public Or {
      if (filters.size() < 2) {
        throw new IllegalArgumentException(
            "And requires at least 2 filters, got " + filters.size());
      }
    }
  }

  record Not(RuleFilter filter) implements RuleFilter {}
}
