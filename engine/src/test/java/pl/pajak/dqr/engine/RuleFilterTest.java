package pl.pajak.dqr.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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
    void allOfKeepsItsOperandsInOrder() {
      assertThat(RuleFilter.allOf(RELEASED, ADDRESS, TAX))
          .isEqualTo(new RuleFilter.And(List.of(RELEASED, ADDRESS, TAX)));
    }

    @Test
    void anyOfKeepsItsOperandsInOrder() {
      assertThat(RuleFilter.anyOf(RELEASED, ADDRESS, TAX))
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
   * A filter is a value, so the list a composite was built from is the caller's and what the
   * composite holds is its own. Otherwise a caller could empty a conjunction the engine is about to
   * match, past the invariant the constructor checked.
   */
  @Nested
  class Immutability {

    @Test
    void aConjunctionKeepsTheOperandsItWasBuiltWith() {
      final var operands = new ArrayList<RuleFilter>(List.of(RELEASED, ADDRESS));

      final var filter = new RuleFilter.And(operands);
      operands.clear();

      assertThat(filter.filters()).containsExactly(RELEASED, ADDRESS);
    }

    @Test
    void aDisjunctionKeepsTheOperandsItWasBuiltWith() {
      final var operands = new ArrayList<RuleFilter>(List.of(RELEASED, ADDRESS));

      final var filter = new RuleFilter.Or(operands);
      operands.clear();

      assertThat(filter.filters()).containsExactly(RELEASED, ADDRESS);
    }

    @Test
    void theOperandsOfACompositeCannotBeChangedThroughIt() {
      final var operands = new RuleFilter.And(List.of(RELEASED, ADDRESS)).filters();

      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(operands::clear);
    }
  }

  /**
   * A conjunction or a disjunction of fewer than two filters says nothing, so every way in defends
   * the invariant: the varargs factories reach the records, and the records refuse the short list.
   */
  @Nested
  class NothingToCombine {

    @Test
    void allOfRejectsNoOperandAtAll() {
      assertThatIllegalArgumentException().isThrownBy(() -> RuleFilter.allOf());
    }

    @Test
    void anyOfRejectsNoOperandAtAll() {
      assertThatIllegalArgumentException().isThrownBy(() -> RuleFilter.anyOf());
    }

    @Test
    void allOfRefusesASingleOperand() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> RuleFilter.allOf(RELEASED))
          .withMessageContaining("at least 2");
    }

    @Test
    void anyOfRefusesASingleOperand() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> RuleFilter.anyOf(RELEASED))
          .withMessageContaining("at least 2");
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
      assertThat(RELEASED.and(ADDRESS)).isEqualTo(RuleFilter.allOf(RELEASED, ADDRESS));
    }

    @Test
    void orBuildsTheSameFilterAsTheFactory() {
      assertThat(RELEASED.or(ADDRESS)).isEqualTo(RuleFilter.anyOf(RELEASED, ADDRESS));
    }

    @Test
    void negateBuildsTheSameFilterAsTheFactory() {
      assertThat(RELEASED.negate()).isEqualTo(RuleFilter.not(RELEASED));
    }

    /**
     * Chaining nests rather than flattens: {@code a.and(b).and(c)} is a conjunction of two, the
     * first of which is itself a conjunction, while {@code allOf(a, b, c)} is a conjunction of
     * three. Both select the same rules, so the difference is one of shape only.
     */
    @Test
    void chainingNestsRatherThanFlattens() {
      assertThat(RELEASED.and(ADDRESS).and(TAX))
          .isEqualTo(
              new RuleFilter.And(List.of(new RuleFilter.And(List.of(RELEASED, ADDRESS)), TAX)))
          .isNotEqualTo(RuleFilter.allOf(RELEASED, ADDRESS, TAX));
    }

    @Test
    void composeIntoTheFilterTheJavadocAdvertises() {
      assertThat(RELEASED.and(ADDRESS.or(TAX)))
          .isEqualTo(RuleFilter.allOf(RELEASED, RuleFilter.anyOf(ADDRESS, TAX)));
    }

    /**
     * The operators serve as method references, which is why the varargs factories are named {@code
     * allOf} and {@code anyOf}: a static and an instance overload sharing a name make {@code
     * RuleFilter::or} ambiguous, and this folding of an unknown number of filters stops compiling.
     */
    @Test
    void serveAsMethodReferencesForFolding() {
      assertThat(Stream.of(RELEASED, ADDRESS, TAX).reduce(RuleFilter::or).orElseThrow())
          .isEqualTo(RELEASED.or(ADDRESS).or(TAX));
      assertThat(Stream.of(RELEASED, ADDRESS, TAX).reduce(RuleFilter::and).orElseThrow())
          .isEqualTo(RELEASED.and(ADDRESS).and(TAX));
    }
  }
}
