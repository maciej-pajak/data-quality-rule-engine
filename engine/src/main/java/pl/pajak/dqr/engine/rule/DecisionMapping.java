package pl.pajak.dqr.engine.rule;

import pl.pajak.dqr.engine.Decision;

/**
 * Turns the value a rule computed into a decision.
 *
 * <p>Data, not logic: no rule computes a decision itself, it computes a value and this mapping says
 * what that value means. A mapping may also decline, in which case the engine reports an error for
 * the record instead of guessing a decision.
 */
public interface DecisionMapping {

  /**
   * Resolves a computed value against this mapping.
   *
   * @param value the value the rule's logic computed for a record
   * @return the decision, or {@link Resolution.Unmapped} when the mapping covers neither the value
   *     nor a default
   */
  Resolution decisionOf(String value);

  /** What the mapping had to say about a computed value. */
  sealed interface Resolution {

    /** A resolution that carries a decision, however the mapping arrived at it. */
    sealed interface Resolved extends Resolution {

      /**
       * Returns the decision the mapping resolved to.
       *
       * @return the decision reported in the result
       */
      Decision decision();

      /**
       * Returns how the decision was arrived at.
       *
       * @return an explanation, reported as the provenance of the result
       */
      String description();
    }

    /**
     * The value is listed in the mapping.
     *
     * @param decision the decision listed for the value
     */
    record Matched(Decision decision) implements Resolved {

      @Override
      public String description() {
        return "the computed value is mapped to " + decision();
      }
    }

    /**
     * The value is not listed, so the mapping's default applies.
     *
     * @param decision the mapping's default decision
     */
    record Defaulted(Decision decision) implements Resolved {

      @Override
      public String description() {
        return "the computed value is not mapped, so the default " + decision() + " applies";
      }
    }

    /**
     * The value is neither listed nor covered by a default, so no decision can be made.
     *
     * @param value the value the mapping does not cover
     */
    record Unmapped(String value) implements Resolution {}
  }
}
