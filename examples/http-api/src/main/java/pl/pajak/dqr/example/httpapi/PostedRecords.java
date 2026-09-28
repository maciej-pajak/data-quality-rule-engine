package pl.pajak.dqr.example.httpapi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pl.pajak.dqr.engine.PartnerRecord;
import pl.pajak.dqr.engine.PartnerRecordSource;

/**
 * Adapts the records of a request to the engine's record source.
 *
 * <p>The posted batch is bounded and already in memory, so the source is a list handed out in
 * batches. A host that runs a million records does not do this — it reads them from where they rest
 * and keeps a batch at a time — but it fills the same seam, which is the point of having one.
 *
 * <p>Every property of a record is text: {@code id} is its reference and everything else, {@code
 * country} included, is reachable through {@link PartnerRecord#getField(String)}. There is no
 * schema, so a caller can post a field the rules have never heard of.
 */
final class PostedRecords {

  /** The property the host reads as the record's reference; every other one is a plain field. */
  private static final String ID_FIELD = "id";

  private PostedRecords() {}

  /**
   * Converts the posted records into a source.
   *
   * @param records the records as posted, each a flat JSON object
   * @return a source that hands them out in batches, then stays empty
   * @throws IllegalArgumentException if a record carries no {@code id}
   */
  static PartnerRecordSource sourceOf(List<Map<String, String>> records) {
    final var converted = records.stream().map(PostedRecords::recordOf).toList();
    return new ListRecordSource(converted);
  }

  private static PartnerRecord recordOf(Map<String, String> properties) {
    final var refId = properties.get(ID_FIELD);
    if (refId == null || refId.isBlank()) {
      throw new IllegalArgumentException("A record carries no " + ID_FIELD + ": " + properties);
    }
    return new PostedRecord(refId, withoutNullValues(properties));
  }

  /** A JSON {@code null} says as little as an absent property, so neither is kept. */
  private static Map<String, String> withoutNullValues(Map<String, String> properties) {
    final var present = new LinkedHashMap<String, String>();
    properties.forEach(
        (name, value) -> {
          if (value != null) {
            present.put(name, value);
          }
        });
    return present;
  }

  /** Hands out a fixed list of records in batches of the size the engine asks for. */
  private static final class ListRecordSource implements PartnerRecordSource {

    private final Deque<PartnerRecord> remaining;

    private ListRecordSource(List<PartnerRecord> records) {
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

  /** One posted object. A field the object does not carry is reported as {@code null}. */
  private record PostedRecord(String refId, Map<String, String> fields) implements PartnerRecord {

    PostedRecord {
      fields = Map.copyOf(fields);
    }

    @Override
    public String getField(String name) {
      return fields.get(name);
    }
  }
}
