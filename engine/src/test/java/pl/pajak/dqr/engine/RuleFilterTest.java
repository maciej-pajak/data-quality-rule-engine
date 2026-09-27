package pl.pajak.dqr.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.engine.rule.RuleStatus;

/**
 * The shapes a filter can be built into. What each shape selects is {@link RuleMatcherTest}'s
 * business.
 */
class RuleFilterTest {

  private static final RuleFilter RELEASED = RuleFilter.status(RuleStatus.RELEASED);
  private static final RuleFilter ADDRESS = RuleFilter.category("address");
  private static final RuleFilter TAX = RuleFilter.category("tax");

  @Nested
  class All {

    @Test
    void carriesNoStateSoOneValueServes() {
      assertThat(RuleFilter.all()).isSameAs(RuleFilter.all()).isSameAs(RuleFilter.All.INSTANCE);
    }
  }

  @Nested
  class Leaves {

    @Test
    void aStatusFilterIsTheStatusItWasGiven() {
      assertThat(RuleFilter.status(RuleStatus.DRAFT))
          .isEqualTo(new RuleFilter.Status(RuleStatus.DRAFT))
          .isNotEqualTo(RELEASED);
    }

    @Test
    void aCategoryFilterIsTheCategoryItWasGiven() {
      assertThat(RuleFilter.category("address"))
          .isEqualTo(new RuleFilter.Category("address"))
          .isNotEqualTo(TAX);
    }
  }

  @Nested
  class Combination {

    @Test
    void andKeepsItsOperandsInOrder() {
      assertThat(RuleFilter.and(RELEASED, ADDRESS, TAX))
          .isEqualTo(new RuleFilter.And(List.of(RELEASED, ADDRESS, TAX)));
    }

    @Test
    void orKeepsItsOperandsInOrder() {
      assertThat(RuleFilter.or(RELEASED, ADDRESS, TAX))
          .isEqualTo(new RuleFilter.Or(List.of(RELEASED, ADDRESS, TAX)));
    }

    @Test
    void notWrapsTheFilterItInverts() {
      assertThat(RuleFilter.not(RELEASED)).isEqualTo(new RuleFilter.Not(RELEASED));
    }

    @Test
    void aNegationIsNotCollapsedWhenNested() {
      assertThat(RuleFilter.not(RuleFilter.not(RELEASED)))
          .isEqualTo(new RuleFilter.Not(new RuleFilter.Not(RELEASED)));
    }
  }

  /**
   * A conjunction or a disjunction of fewer than two filters says nothing, and the API leaves no
   * way to build one by accident: a single-argument {@code RuleFilter.and(f)} resolves to the
   * instance operator rather than the varargs factory, so only the empty call and the records
   * themselves have to defend the invariant.
   */
  @Nested
  class NothingToCombine {

    @Test
    void andRejectsNoOperandAtAll() {
      assertThatIllegalArgumentException().isThrownBy(() -> RuleFilter.and());
    }

    @Test
    void orRejectsNoOperandAtAll() {
      assertThatIllegalArgumentException().isThrownBy(() -> RuleFilter.or());
    }

    @Test
    void andRefusesASingleOperand() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new RuleFilter.And(List.of(RELEASED)))
          .withMessageContaining("at least 2");
    }

    @Test
    void orRefusesASingleOperand() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new RuleFilter.Or(List.of(RELEASED)))
          .withMessageContaining("at least 2");
    }

    @Test
    void anEmptyListIsRefusedTheSameWay() {
      assertThatIllegalArgumentException().isThrownBy(() -> new RuleFilter.And(List.of()));
      assertThatIllegalArgumentException().isThrownBy(() -> new RuleFilter.Or(List.of()));
    }
  }

  /** The fluent operators are the static factories; a filter reads the same either way. */
  @Nested
  class FluentOperators {

    @Test
    void andBuildsTheSameFilterAsTheFactory() {
      assertThat(RELEASED.and(ADDRESS)).isEqualTo(RuleFilter.and(RELEASED, ADDRESS));
    }

    @Test
    void orBuildsTheSameFilterAsTheFactory() {
      assertThat(RELEASED.or(ADDRESS)).isEqualTo(RuleFilter.or(RELEASED, ADDRESS));
    }

    @Test
    void negateBuildsTheSameFilterAsTheFactory() {
      assertThat(RELEASED.negate()).isEqualTo(RuleFilter.not(RELEASED));
    }

    /**
     * Chaining nests rather than flattens: {@code a.and(b).and(c)} is a conjunction of two, the
     * first of which is itself a conjunction, while {@code and(a, b, c)} is a conjunction of three.
     * Both select the same rules, so the difference is one of shape only.
     */
    @Test
    void chainingNestsRatherThanFlattens() {
      assertThat(RELEASED.and(ADDRESS).and(TAX))
          .isEqualTo(
              new RuleFilter.And(List.of(new RuleFilter.And(List.of(RELEASED, ADDRESS)), TAX)))
          .isNotEqualTo(RuleFilter.and(RELEASED, ADDRESS, TAX));
    }

    @Test
    void composeIntoTheFilterTheJavadocAdvertises() {
      assertThat(RELEASED.and(ADDRESS.or(TAX)))
          .isEqualTo(RuleFilter.and(RELEASED, RuleFilter.or(ADDRESS, TAX)));
    }
  }
}
