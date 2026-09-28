package pl.pajak.dqr.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.engine.rule.Rule;
import pl.pajak.dqr.engine.testing.FakeRuleCatalog;
import pl.pajak.dqr.engine.testing.TestRecord;
import pl.pajak.dqr.engine.testing.TestRule;

/**
 * A run over a volume large enough that accumulation would show, to hold the claim the library
 * makes about scale.
 *
 * <p>Nothing here is proportional to the number of records: the source mints each record when the
 * engine asks for it and forgets it afterwards, and the sink counts and discards. Bounded memory is
 * a property of the loop, not of the number, so a volume that runs in a second demonstrates it as
 * well as a million would — {@link #streamsResultsWhileTheSourceStillHasRecords()} is what makes
 * the claim falsifiable, since an engine that accumulated would leave the sink empty until the
 * source was drained.
 */
class RuleEngineVolumeTest {

  private static final int RECORD_COUNT = 100_000;

  private static final int BATCH_SIZE = 1_000;

  private static final int BATCHES = RECORD_COUNT / BATCH_SIZE;

  @Test
  void decidesEveryRecordOfALargeSource() {
    final var source = new GeneratedRecordSource(RECORD_COUNT);
    final var sink = new CountingSink();

    final var summary = run(source, sink, threeRules());

    assertThat(source.mintedRecords()).isEqualTo(RECORD_COUNT);
    assertThat(summary.resultsCount()).isEqualTo(RECORD_COUNT * 3);
    assertThat(summary.errorsCount()).isZero();
    assertThat(sink.resultsCount()).isEqualTo(summary.resultsCount());
    assertThat(summary.resultsCountByDecision())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(Decision.VALID, RECORD_COUNT * 2, Decision.INVALID, RECORD_COUNT));
  }

  @Test
  void holdsOneBatchAtATimeWhateverTheSourceSize() {
    final var source = new GeneratedRecordSource(RECORD_COUNT);

    run(source, new CountingSink(), threeRules());

    assertThat(source.requestedSizes()).containsOnly(BATCH_SIZE);
    assertThat(source.largestBatchHandedOut())
        .as("the engine never asks for more than the batch size it was configured with")
        .isEqualTo(BATCH_SIZE);
    assertThat(source.readCount())
        .as("one read per batch, plus the empty read that ends the run")
        .isEqualTo(BATCHES + 1);
  }

  @Test
  void streamsResultsWhileTheSourceStillHasRecords() {
    final var sink = new CountingSink();
    final var source = new GeneratedRecordSource(RECORD_COUNT);
    source.observeBefore(sink::resultsCount);

    run(source, sink, threeRules());

    final var halfway = BATCHES / 2;
    assertThat(source.observedAtRead(halfway))
        .as("the results of every batch read before this one")
        .isEqualTo((halfway - 1) * BATCH_SIZE * 3);
    assertThat(source.mintedRecordsAtRead(halfway))
        .as("and the source has not been drained yet")
        .isLessThan(RECORD_COUNT);
  }

  @Test
  void isolatesAFailingRuleAcrossTheWholeSource() {
    final var sink = new CountingSink();

    final var summary =
        run(
            new GeneratedRecordSource(RECORD_COUNT),
            sink,
            List.of(
                TestRule.builder("valid").computing("ok").deciding(Decision.VALID).build(),
                TestRule.builder("broken").throwing(new IllegalStateException("no")).build()));

    assertThat(summary.resultsCount()).isEqualTo(RECORD_COUNT);
    assertThat(summary.errorsCount()).isEqualTo(RECORD_COUNT);
    assertThat(sink.errorsCount()).isEqualTo(RECORD_COUNT);
  }

  private static RunSummary run(PartnerRecordSource source, RuleResultSink sink, List<Rule> rules) {
    return RuleEngine.newInstance()
        .process(
            EngineInput.builder(new FakeRuleCatalog(rules), source, sink)
                .withBatchSize(BATCH_SIZE)
                .build());
  }

  private static List<Rule> threeRules() {
    return List.of(
        TestRule.builder("first").computing("ok").deciding(Decision.VALID).build(),
        TestRule.builder("second").computing("ok").deciding(Decision.VALID).build(),
        TestRule.builder("third").computing("bad").deciding(Decision.INVALID).build());
  }

  /**
   * A source that mints records on demand instead of holding them. It keeps counters only, so the
   * test itself stays bounded too, and can look at the run through a caller-supplied probe every
   * time the engine asks for a batch.
   */
  private static final class GeneratedRecordSource implements PartnerRecordSource {

    private final int recordCount;
    private final List<Integer> requestedSizes = new ArrayList<>();
    private final List<Integer> observations = new ArrayList<>();
    private final List<Integer> mintedAtRead = new ArrayList<>();

    private IntSupplier probe = () -> 0;
    private int minted = 0;
    private int largestBatch = 0;

    private GeneratedRecordSource(int recordCount) {
      this.recordCount = recordCount;
    }

    /** Reads {@code probe} at the start of every batch, before any record of it is handed out. */
    void observeBefore(IntSupplier probe) {
      this.probe = probe;
    }

    @Override
    public List<PartnerRecord> readNextBatch(int maxRecords) {
      requestedSizes.add(maxRecords);
      observations.add(probe.getAsInt());
      mintedAtRead.add(minted);

      final var batchSize = Math.min(maxRecords, recordCount - minted);
      final var batch = new ArrayList<PartnerRecord>(batchSize);
      for (int i = 0; i < batchSize; i++) {
        batch.add(TestRecord.inCountry("r" + minted++, "DE"));
      }
      largestBatch = Math.max(largestBatch, batch.size());
      return List.copyOf(batch);
    }

    List<Integer> requestedSizes() {
      return List.copyOf(requestedSizes);
    }

    int readCount() {
      return requestedSizes.size();
    }

    int mintedRecords() {
      return minted;
    }

    int largestBatchHandedOut() {
      return largestBatch;
    }

    /** What the probe answered at the {@code n}-th read, counting from one. */
    int observedAtRead(int n) {
      return observations.get(n - 1);
    }

    /** How many records had been minted when the {@code n}-th read began, counting from one. */
    int mintedRecordsAtRead(int n) {
      return mintedAtRead.get(n - 1);
    }
  }

  /**
   * A sink that keeps counts and nothing else, which is what a host processing a source of this
   * size would do with anything it cannot stream straight out.
   */
  private static final class CountingSink implements RuleResultSink {

    private int results = 0;
    private int errors = 0;

    @Override
    public void onResult(RuleResult result) {
      results++;
    }

    @Override
    public void onError(RuleError error) {
      errors++;
    }

    int resultsCount() {
      return results;
    }

    int errorsCount() {
      return errors;
    }
  }
}
