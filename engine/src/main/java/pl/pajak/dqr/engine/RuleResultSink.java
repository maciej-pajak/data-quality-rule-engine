package pl.pajak.dqr.engine;

/**
 * Receives the outcomes of a run as the engine produces them.
 *
 * <p>Results are streamed rather than collected: the engine never holds the outcomes of a run, so a
 * sink can write them to a response, a file or a queue of any size. Both methods are called on the
 * thread that called {@link RuleEngineImpl#process(EngineInput)}, one call at a time, so an
 * implementation needs no synchronisation of its own.
 *
 * <p>Implementations should not throw. An exception from {@link #onResult(RuleResult)} is reported
 * as a {@link RuleError} for the record and rule being evaluated; an exception from {@link
 * #onError(RuleError)} ends the run.
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
