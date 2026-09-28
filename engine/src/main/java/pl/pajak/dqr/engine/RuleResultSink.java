package pl.pajak.dqr.engine;

/**
 * Receives the outcomes of a run as the engine produces them.
 *
 * <p>Both methods are called on the thread that called {@link RuleEngine#process(EngineInput)}, one
 * call at a time, so an implementation needs no synchronisation of its own.
 *
 * <p>Implementations should not throw: an exception from either method ends the run as a {@link
 * ResultSinkException}. A sink that would rather drop single outcomes than end the run catches
 * inside these methods, and counts what it dropped itself — the {@link RunSummary} counts only the
 * outcomes the sink accepted.
 *
 * @see RunSummary
 */
public interface RuleResultSink {

  /**
   * Called once for every rule evaluated against every record it applies to.
   *
   * @param result the outcome of a single rule on a single record
   */
  void onResult(RuleResult result);

  /**
   * Called once for every rule that failed on a record; the run continues with the remaining rules
   * and records.
   *
   * @param error the failure of a single rule on a single record
   */
  void onError(RuleError error);
}
