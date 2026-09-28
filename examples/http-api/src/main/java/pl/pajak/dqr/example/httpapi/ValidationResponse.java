package pl.pajak.dqr.example.httpapi;

import java.util.List;
import java.util.Map;
import pl.pajak.dqr.engine.Decision;
import pl.pajak.dqr.engine.RuleError;
import pl.pajak.dqr.engine.RuleResult;
import pl.pajak.dqr.engine.RunSummary;
import pl.pajak.dqr.engine.rule.RuleSeverity;

/**
 * What one run answers: the results, the failures that did not stop it, and the summary.
 *
 * <p>The host's own wire shape, not the engine's types serialised: the API can then be versioned
 * without the library's accessors leaking into it, and {@link RuleError#cause()} becomes a class
 * name rather than a serialised stack.
 *
 * @param results what each rule decided about each record it applied to
 * @param errors the rules that failed, one entry per rule and record
 * @param summary the counts of the run
 */
record ValidationResponse(List<Result> results, List<Failure> errors, Summary summary) {

  /** What one rule decided about one record, with the provenance that explains it. */
  record Result(
      String recordRefId,
      String ruleId,
      String ruleLabel,
      Decision decision,
      RuleSeverity severity,
      Provenance provenance) {

    static Result of(RuleResult result) {
      return new Result(
          result.recordRefId(),
          result.ruleId(),
          result.ruleLabel(),
          result.decision(),
          result.severity(),
          Provenance.of(result.provenance()));
    }
  }

  /** How a decision was reached: the computed value, the mapping's word on it, the data read. */
  record Provenance(String computedValue, String description, Map<String, String> accessedFields) {

    static Provenance of(RuleResult.Provenance provenance) {
      return new Provenance(
          provenance.computedValue(), provenance.description(), provenance.accessedFields());
    }
  }

  /**
   * One rule that failed on one record; the rest of the run went on without it.
   *
   * <p>Named apart from the engine's {@code RuleError} to keep {@code Error} out of the wire
   * shape's namespace.
   */
  record Failure(
      String recordRefId, String ruleId, String ruleLabel, String message, String cause) {

    static Failure of(RuleError error) {
      return new Failure(
          error.recordRefId(),
          error.ruleId(),
          error.ruleLabel(),
          error.message(),
          error.cause().map(cause -> cause.getClass().getName()).orElse(null));
    }
  }

  /** The counts of the run, as the engine reported them. */
  record Summary(
      int resultsCount,
      int errorsCount,
      Map<Decision, Integer> resultsCountByDecision,
      Map<RuleSeverity, Integer> resultsCountBySeverity) {

    static Summary of(RunSummary summary) {
      return new Summary(
          summary.resultsCount(),
          summary.errorsCount(),
          summary.resultsCountByDecision(),
          summary.resultsCountBySeverity());
    }
  }
}
