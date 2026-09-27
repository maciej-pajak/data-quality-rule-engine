package pl.pajak.rule.engine.testing;

import java.util.LinkedHashMap;
import java.util.Map;
import pl.pajak.rule.engine.PartnerRecord;

/** A partner record assembled for a test: a reference and flat text fields. */
public record TestRecord(String refId, Map<String, String> fields) implements PartnerRecord {

  /** A record that carries no fields, not even a country. */
  public static PartnerRecord of(String refId) {
    return new TestRecord(refId, Map.of());
  }

  /** A record whose country field holds one country code. */
  public static PartnerRecord inCountry(String refId, String countryCode) {
    return new TestRecord(refId, Map.of("country", countryCode));
  }

  public static Builder builder(String refId) {
    return new Builder(refId);
  }

  @Override
  public String getField(String name) {
    return fields.get(name);
  }

  public static final class Builder {

    private final String refId;
    private final Map<String, String> fields = new LinkedHashMap<>();

    private Builder(String refId) {
      this.refId = refId;
    }

    public Builder inCountry(String countryCode) {
      return withField("country", countryCode);
    }

    public Builder withField(String name, String value) {
      fields.put(name, value);
      return this;
    }

    public PartnerRecord build() {
      return new TestRecord(refId, Map.copyOf(fields));
    }
  }
}
