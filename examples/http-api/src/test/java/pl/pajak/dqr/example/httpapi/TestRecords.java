package pl.pajak.dqr.example.httpapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** The record documents the tests post, as text: what a caller sends is a document, not a type. */
final class TestRecords {

  private static final String RECORDS_RESOURCE = "fixtures/records.json";

  private TestRecords() {}

  /** The task's own document, posted as it is written. */
  static String fixtureBatch() {
    try (InputStream source =
        TestRecords.class.getClassLoader().getResourceAsStream(RECORDS_RESOURCE)) {
      if (source == null) {
        throw new AssertionError("Records resource not found: " + RECORDS_RESOURCE);
      }
      return new String(source.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new AssertionError("Cannot read " + RECORDS_RESOURCE, ex);
    }
  }

  /** A batch of valid records, for the cases that are about its size rather than its content. */
  static String validBatch(int count) {
    return IntStream.rangeClosed(1, count)
        .mapToObj(
            i ->
                """
                {"id": "r%d", "vatId": "DE11111111%d", "country": "DE", "iban": "DE1234"}"""
                    .formatted(i, i))
        .collect(Collectors.joining(",", "[", "]"));
  }
}
