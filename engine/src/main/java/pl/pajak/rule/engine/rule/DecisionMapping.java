package pl.pajak.rule.engine.rule;

import pl.pajak.rule.engine.Decision;

/**
 * Turns the value a rule computed into a decision. Data, not logic: no rule computes a decision.
 */
public interface DecisionMapping {

  /** Resolves {@code value} against the mapping. */
  Resolution decisionOf(String value);

  /** What the mapping had to say about a computed value. */
  sealed interface Resolution {

    /** A resolution that carries a decision, however the mapping arrived at it. */
    sealed interface Resolved extends Resolution {

      /** The decision the mapping resolved to. */
      Decision decision();

      /** How the decision was arrived at, for the provenance of the result. */
      String description();
    }

    /** The value is listed in the mapping. */
    record Matched(Decision decision) implements Resolved {

      @Override
      public String description() {
        return "the computed value is mapped to " + decision();
      }
    }

    /** The value is not listed, so the mapping's default applies. */
    record Defaulted(Decision decision) implements Resolved {

      @Override
      public String description() {
        return "the computed value is not mapped, so the default " + decision() + " applies";
      }
    }

    /** The value is neither listed nor covered by a default, so no decision can be made. */
    record Unmapped(String value) implements Resolution {}
  }
}
