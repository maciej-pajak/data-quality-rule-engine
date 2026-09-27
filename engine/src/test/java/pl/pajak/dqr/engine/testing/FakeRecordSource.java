package pl.pajak.dqr.engine.testing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import pl.pajak.dqr.engine.PartnerRecord;
import pl.pajak.dqr.engine.PartnerRecordSource;

/**
 * A finite source that hands out a fixed list in batches and remembers how it was asked, so a test
 * can assert on the engine's read loop.
 */
public final class FakeRecordSource implements PartnerRecordSource {

  private final Deque<PartnerRecord> remaining;
  private final List<Integer> requestedSizes = new ArrayList<>();

  public FakeRecordSource(List<PartnerRecord> records) {
    this.remaining = new ArrayDeque<>(records);
  }

  public static FakeRecordSource of(PartnerRecord... records) {
    return new FakeRecordSource(List.of(records));
  }

  /** A source that is exhausted from the start. */
  public static FakeRecordSource empty() {
    return new FakeRecordSource(List.of());
  }

  @Override
  public List<PartnerRecord> readNextBatch(int maxRecords) {
    requestedSizes.add(maxRecords);
    final var batch = new ArrayList<PartnerRecord>(Math.min(maxRecords, remaining.size()));
    while (batch.size() < maxRecords && !remaining.isEmpty()) {
      batch.add(remaining.poll());
    }
    return List.copyOf(batch);
  }

  /** The {@code maxRecords} of every read so far, in order. */
  public List<Integer> requestedSizes() {
    return List.copyOf(requestedSizes);
  }

  public int readCount() {
    return requestedSizes.size();
  }

  /** Records not handed out yet. Zero once the source is drained. */
  public int remainingRecords() {
    return remaining.size();
  }
}
