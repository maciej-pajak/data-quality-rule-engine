package pl.pajak.rule.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.pajak.rule.engine.rule.CountryScope;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleStatus;
import pl.pajak.rule.engine.testing.TestRule;

class RuleMatcherTest {

  private static final Rule RELEASED_ADDRESS_RULE =
      rule(RuleStatus.RELEASED, List.of("address", "completeness"));

  @Nested
  class All {

    @Test
    void matchesEveryRule() {
      assertThat(RuleMatcher.matches(RuleFilter.all(), RELEASED_ADDRESS_RULE)).isTrue();
      assertThat(RuleMatcher.matches(RuleFilter.all(), rule(RuleStatus.DRAFT, List.of()))).isTrue();
    }
  }

  @Nested
  class Category {

    @Test
    void matchesWhenTheRuleCarriesTheCategory() {
      assertThat(RuleMatcher.matches(RuleFilter.category("address"), RELEASED_ADDRESS_RULE))
          .isTrue();
    }

    @Test
    void matchesOnAnySecondaryCategoryToo() {
      assertThat(RuleMatcher.matches(RuleFilter.category("completeness"), RELEASED_ADDRESS_RULE))
          .isTrue();
    }

    @Test
    void doesNotMatchAnUnknownCategory() {
      assertThat(RuleMatcher.matches(RuleFilter.category("tax"), RELEASED_ADDRESS_RULE)).isFalse();
    }

    @Test
    void isCaseSensitive() {
      assertThat(RuleMatcher.matches(RuleFilter.category("Address"), RELEASED_ADDRESS_RULE))
          .isFalse();
    }

    @Test
    void doesNotMatchARuleWithoutCategories() {
      assertThat(RuleMatcher.matches(RuleFilter.category("address"), rule(RuleStatus.RELEASED)))
          .isFalse();
    }
  }

  @Nested
  class Status {

    @Test
    void matchesTheSameStatus() {
      assertThat(RuleMatcher.matches(RuleFilter.status(RuleStatus.RELEASED), RELEASED_ADDRESS_RULE))
          .isTrue();
    }

    @Test
    void doesNotMatchAnotherStatus() {
      assertThat(RuleMatcher.matches(RuleFilter.status(RuleStatus.DRAFT), RELEASED_ADDRESS_RULE))
          .isFalse();
      assertThat(
              RuleMatcher.matches(RuleFilter.status(RuleStatus.DEPRECATED), RELEASED_ADDRESS_RULE))
          .isFalse();
    }
  }

  @Nested
  class And {

    @Test
    void matchesWhenEveryOperandMatches() {
      RuleFilter filter =
          RuleFilter.and(RuleFilter.category("address"), RuleFilter.status(RuleStatus.RELEASED));

      assertThat(RuleMatcher.matches(filter, RELEASED_ADDRESS_RULE)).isTrue();
    }

    @Test
    void doesNotMatchWhenOneOperandFails() {
      RuleFilter filter =
          RuleFilter.and(RuleFilter.category("address"), RuleFilter.status(RuleStatus.DRAFT));

      assertThat(RuleMatcher.matches(filter, RELEASED_ADDRESS_RULE)).isFalse();
    }
  }

  @Nested
  class Or {

    @Test
    void matchesWhenOneOperandMatches() {
      RuleFilter filter =
          RuleFilter.or(RuleFilter.category("tax"), RuleFilter.status(RuleStatus.RELEASED));

      assertThat(RuleMatcher.matches(filter, RELEASED_ADDRESS_RULE)).isTrue();
    }

    @Test
    void doesNotMatchWhenNoOperandMatches() {
      RuleFilter filter =
          RuleFilter.or(RuleFilter.category("tax"), RuleFilter.status(RuleStatus.DRAFT));

      assertThat(RuleMatcher.matches(filter, RELEASED_ADDRESS_RULE)).isFalse();
    }
  }

  @Nested
  class Not {

    @Test
    void invertsAMatch() {
      assertThat(RuleMatcher.matches(RuleFilter.not(RuleFilter.all()), RELEASED_ADDRESS_RULE))
          .isFalse();
    }

    @Test
    void invertsAMiss() {
      assertThat(
              RuleMatcher.matches(
                  RuleFilter.not(RuleFilter.category("tax")), RELEASED_ADDRESS_RULE))
          .isTrue();
    }

    @Test
    void cancelsOutWhenNested() {
      assertThat(
              RuleMatcher.matches(
                  RuleFilter.not(RuleFilter.not(RuleFilter.category("address"))),
                  RELEASED_ADDRESS_RULE))
          .isTrue();
    }
  }

  @Nested
  class Composition {

    private final RuleFilter filter =
        RuleFilter.and(
            RuleFilter.or(RuleFilter.category("address"), RuleFilter.category("tax")),
            RuleFilter.not(RuleFilter.status(RuleStatus.DEPRECATED)));

    @Test
    void matchesARuleSatisfyingBothBranches() {
      assertThat(RuleMatcher.matches(filter, RELEASED_ADDRESS_RULE)).isTrue();
    }

    @Test
    void doesNotMatchARuleFailingTheNegatedBranch() {
      Rule deprecated = rule(RuleStatus.DEPRECATED, List.of("tax"));

      assertThat(RuleMatcher.matches(filter, deprecated)).isFalse();
    }

    @Test
    void doesNotMatchARuleFailingTheDisjunction() {
      Rule other = rule(RuleStatus.RELEASED, List.of("naming"));

      assertThat(RuleMatcher.matches(filter, other)).isFalse();
    }

    @Test
    void readsTheSameThroughTheFluentOperators() {
      RuleFilter fluent =
          RuleFilter.category("address")
              .or(RuleFilter.category("tax"))
              .and(RuleFilter.status(RuleStatus.DEPRECATED).negate());

      assertThat(RuleMatcher.matches(fluent, RELEASED_ADDRESS_RULE)).isTrue();
      assertThat(RuleMatcher.matches(fluent, rule(RuleStatus.DEPRECATED, List.of("tax"))))
          .isFalse();
    }
  }

  private static Rule rule(RuleStatus status, String... categories) {
    return TestRule.builder("test-rule")
        .withStatus(status)
        .withCategories(categories)
        .withCountryScope(CountryScope.World)
        .build();
  }

  private static Rule rule(RuleStatus status, List<String> categories) {
    return TestRule.builder("test-rule")
        .withStatus(status)
        .withCategories(categories)
        .withCountryScope(CountryScope.World)
        .build();
  }
}
