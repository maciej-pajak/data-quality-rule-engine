package pl.pajak.dqr.rule.java.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pl.pajak.rule.engine.PartnerRecord;

/**
 * Partner records read from a JSON array of flat objects, the shape the task description uses.
 *
 * <p>Every property is text: {@code id} is the record's reference and every property, {@code
 * country} included, is reachable through {@link PartnerRecord#getField(String)}. The record has no
 * schema, so a fixture can add a field without a change here.
 */
public final class JsonPartnerRecords {

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final TypeReference<List<Map<String, String>>> ROWS = new TypeReference<>() {};

  private static final String ID_FIELD = "id";

  private JsonPartnerRecords() {}

  /** Reads the records from a classpath resource. */
  public static List<PartnerRecord> fromClasspath(String resource) {
    try (InputStream source =
        JsonPartnerRecords.class.getClassLoader().getResourceAsStream(resource)) {
      if (source == null) {
        throw new IllegalStateException("Records resource not found: " + resource);
      }
      return JSON.<List<Map<String, String>>>readValue(source, ROWS).stream()
          .map(JsonPartnerRecords::recordOf)
          .toList();
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot read records from " + resource, ex);
    }
  }

  private static PartnerRecord recordOf(Map<String, String> fields) {
    final var refId = fields.get(ID_FIELD);
    if (refId == null || refId.isBlank()) {
      throw new IllegalStateException("Record without an id: " + fields);
    }
    return new JsonPartnerRecord(refId, withoutNullValues(fields));
  }

  /** A JSON {@code null} says as little as an absent property, so neither is kept. */
  private static Map<String, String> withoutNullValues(Map<String, String> fields) {
    final var present = new LinkedHashMap<String, String>();
    fields.forEach(
        (name, value) -> {
          if (value != null) {
            present.put(name, value);
          }
        });
    return present;
  }

  /** One JSON object. A property the object does not carry is reported as {@code null}. */
  private record JsonPartnerRecord(String refId, Map<String, String> fields)
      implements PartnerRecord {

    JsonPartnerRecord {
      fields = Map.copyOf(fields);
    }

    @Override
    public String getField(String name) {
      return fields.get(name);
    }
  }
}
