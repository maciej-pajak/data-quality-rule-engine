package pl.pajak.dqr.engine;

/**
 * Validates business-partner records against a catalog of data-quality rules.
 *
 * <p>A run is described by an {@link EngineInput} and driven by {@link #process(EngineInput)}: the
 * engine loads the catalog once, keeps the rules the input's {@link RuleFilter} selects, then reads
 * the records in batches and evaluates every selected rule against every record of a batch. Only
 * one batch is held at a time, and outcomes are handed to the input's {@link RuleResultSink} as
 * they are produced, so a run over a source of any size needs no more memory than a batch.
 *
 * <p>An engine holds no state between runs, so one instance can serve any number of them; whether
 * two runs may overlap depends on the catalog, source and sink they are given.
 *
 * <pre>{@code
 * RunSummary summary =
 *     RuleEngine.newInstance().process(EngineInput.builder(catalog, source, sink).build());
 * }</pre>
 */
public sealed interface RuleEngine permits RuleEngineImpl {

  /**
   * Creates an engine.
   *
   * @return a new engine, ready to process any input
   */
  static RuleEngine newInstance() {
    return new RuleEngineImpl();
  }

  /**
   * Runs the selected rules of the catalog against the records of the source.
   *
   * <p>Records are evaluated on the calling thread and the call returns once the source is
   * exhausted. A rule that fails on a record does not stop the run: the failure is reported to the
   * sink as a {@link RuleError} and the remaining rules and records are evaluated. A rule whose
   * country scope does not cover a record is skipped for that record, producing neither a result
   * nor an error.
   *
   * @param engineInput the configuration of the run
   * @return the counts of what the run produced
   * @throws RuntimeException if the catalog cannot be loaded, the source cannot deliver a batch, or
   *     the sink fails to accept an error
   */
  RunSummary process(EngineInput engineInput);
}
