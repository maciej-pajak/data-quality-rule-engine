package pl.pajak.dqr.rule.java.testing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import pl.pajak.dqr.engine.PartnerRecord;
import pl.pajak.dqr.engine.PartnerRecordSource;

/** A finite source that hands out a fixed list of records in batches, then stays empty. */
public final class ListRecordSource implements PartnerRecordSource {

  private final Deque<PartnerRecord> remaining;

  public ListRecordSource(List<PartnerRecord> records) {
    this.remaining = new ArrayDeque<>(records);
  }

  @Override
  public List<PartnerRecord> readNextBatch(int maxRecords) {
    final var batch = new ArrayList<PartnerRecord>(Math.min(maxRecords, remaining.size()));
    while (batch.size() < maxRecords && !remaining.isEmpty()) {
      batch.add(remaining.poll());
    }
    return List.copyOf(batch);
  }
}
