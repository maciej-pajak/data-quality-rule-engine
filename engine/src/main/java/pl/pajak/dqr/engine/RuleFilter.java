package pl.pajak.dqr.engine;

import java.util.Arrays;
import java.util.List;
import pl.pajak.dqr.engine.rule.RuleStatus;

/**
 * Selects which rules of a catalog take part in a run.
 *
 * <p>A filter is a value, not a predicate the caller implements: the engine matches it against
 * every rule the catalog loaded, and only the matching rules are evaluated. Filters compose through
 * {@link #and(RuleFilter)}, {@link #or(RuleFilter)} and {@link #negate()}, so a run can be narrowed
 * without a new filter kind.
 *
 * <p>Unless {@link EngineInput.Builder#withRuleFilter(RuleFilter)} says otherwise, a run uses
 * {@code status(RuleStatus.RELEASED)}.
 *
 * <pre>{@code
 * RuleFilter filter = RuleFilter.status(RuleStatus.RELEASED)
 *     .and(RuleFilter.category("address").or(RuleFilter.category("tax")));
 * }</pre>
 *
 * <p>Implementations are immutable and the hierarchy is sealed; callers use the factory methods
 * rather than the permitted types.
 */
public sealed interface RuleFilter {

  /**
   * Returns a filter that matches every rule of the catalog.
   *
   * @return a filter matching all rules
   */
  static RuleFilter all() {
    return RuleFilter.All.INSTANCE;
  }

  /**
   * Returns a filter that matches rules with the given status.
   *
   * @param status the status a rule must have
   * @return a filter on the rule's status
   */
  static RuleFilter status(RuleStatus status) {
    return new RuleFilter.Status(status);
  }

  /**
   * Returns a filter that matches rules carrying the given category.
   *
   * <p>A rule may declare several categories; it matches when the category is among them.
   *
   * @param category the category a rule must carry
   * @return a filter on the rule's categories
   */
  static RuleFilter category(String category) {
    return new RuleFilter.Category(category);
  }

  /**
   * Returns a filter that matches a rule only when all the given filters match.
   *
   * <p>Named apart from the instance {@link #and(RuleFilter)} on purpose: a static and an instance
   * overload of one name cannot both be a method reference target, so {@code RuleFilter::and} would
   * not compile.
   *
   * @param filters the filters to combine; at least two
   * @return the conjunction of the filters
   * @throws IllegalArgumentException if fewer than two filters are given
   */
  static RuleFilter allOf(RuleFilter... filters) {
    return new RuleFilter.And(RuleFilter.of(filters));
  }

  /**
   * Returns a filter that matches a rule when at least one of the given filters matches.
   *
   * <p>Named apart from the instance {@link #or(RuleFilter)} on purpose: see {@link
   * #allOf(RuleFilter...)}.
   *
   * @param filters the filters to combine; at least two
   * @return the disjunction of the filters
   * @throws IllegalArgumentException if fewer than two filters are given
   */
  static RuleFilter anyOf(RuleFilter... filters) {
    return new RuleFilter.Or(RuleFilter.of(filters));
  }

  /**
   * Returns a filter that matches the rules the given filter does not.
   *
   * @param filter the filter to invert
   * @return the negation of the filter
   */
  static RuleFilter not(RuleFilter filter) {
    return new RuleFilter.Not(filter);
  }

  /**
   * Returns a filter that matches a rule only when this filter and {@code other} match.
   *
   * @param other the filter to combine with this one
   * @return the conjunction of the two filters
   */
  default RuleFilter and(RuleFilter other) {
    return new And(List.of(this, other));
  }

  /**
   * Returns a filter that matches a rule when this filter or {@code other} matches.
   *
   * @param other the filter to combine with this one
   * @return the disjunction of the two filters
   */
  default RuleFilter or(RuleFilter other) {
    return new Or(List.of(this, other));
  }

  /**
   * Returns a filter that matches the rules this one does not.
   *
   * @return the negation of this filter
   */
  default RuleFilter negate() {
    return new Not(this);
  }

  /** Matches every rule. Obtained from {@link RuleFilter#all()}. */
  final class All implements RuleFilter {

    /** The single instance; the filter carries no state. */
    public static final RuleFilter.All INSTANCE = new RuleFilter.All();

    private All() {}
  }

  /**
   * Matches rules with one status. Obtained from {@link RuleFilter#status(RuleStatus)}.
   *
   * @param status the status a rule must have
   */
  record Status(RuleStatus status) implements RuleFilter {}

  /**
   * Matches rules carrying one category. Obtained from {@link RuleFilter#category(String)}.
   *
   * @param category the category a rule must carry
   */
  record Category(String category) implements RuleFilter {}

  /**
   * Matches rules that all its filters match. Obtained from {@link
   * RuleFilter#allOf(RuleFilter...)}.
   *
   * @param filters the combined filters; at least two
   */
  record And(List<RuleFilter> filters) implements RuleFilter {

    /**
     * Checks that there is something to combine.
     *
     * @throws IllegalArgumentException if fewer than two filters are given
     */
    public And {
      if (filters.size() < 2) {
        throw new IllegalArgumentException(
            "And requires at least 2 filters, got " + filters.size());
      }
    }
  }

  /**
   * Matches rules that at least one of its filters matches. Obtained from {@link
   * RuleFilter#anyOf(RuleFilter...)}.
   *
   * @param filters the combined filters; at least two
   */
  record Or(List<RuleFilter> filters) implements RuleFilter {

    /**
     * Checks that there is something to combine.
     *
     * @throws IllegalArgumentException if fewer than two filters are given
     */
    public Or {
      if (filters.size() < 2) {
        throw new IllegalArgumentException("Or requires at least 2 filters, got " + filters.size());
      }
    }
  }

  /**
   * Matches the rules its filter does not. Obtained from {@link RuleFilter#not(RuleFilter)}.
   *
   * @param filter the inverted filter
   */
  record Not(RuleFilter filter) implements RuleFilter {}

  private static List<RuleFilter> of(RuleFilter... filters) {
    return Arrays.stream(filters).toList();
  }
}
