package pl.pajak.dqr.engine;

import java.util.List;

/**
 * Supplies the records of a run to the engine, one batch at a time.
 *
 * <p>The engine keeps requesting batches until an empty one is returned, so a source is not
 * required to know how many records it holds.
 *
 * @see EngineInput#builder(pl.pajak.dqr.engine.rule.RuleCatalog, PartnerRecordSource,
 *     RuleResultSink)
 */
public interface PartnerRecordSource {

  /**
   * Returns the next batch of records, or an empty list once the source is exhausted.
   *
   * <p>A batch shorter than requested does not end the run: only an empty batch does. Each batch is
   * evaluated against every rule of the run before the next one is requested.
   *
   * @param maxRecords the maximum number of records to return; always positive
   * @return the records read, never {@code null}
   * @throws RuntimeException if the batch cannot be read; unlike a failure of a single record, this
   *     is not contained by the engine and ends the run
   */
  List<PartnerRecord> readNextBatch(int maxRecords);
}
