package pl.pajak.rule.engine.testing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import pl.pajak.rule.engine.PartnerRecord;
import pl.pajak.rule.engine.rule.CountryScope;

/** A partner record assembled for a test: a reference, an optional country and flat text fields. */
public record TestRecord(String refId, Optional<CountryScope> country, Map<String, String> fields)
    implements PartnerRecord {

  /** A record that belongs to no country and carries no fields. */
  public static PartnerRecord of(String refId) {
    return new TestRecord(refId, Optional.empty(), Map.of());
  }

  /** A record scoped to one country code. */
  public static PartnerRecord inCountry(String refId, String countryCode) {
    return new TestRecord(refId, Optional.of(new CountryScope.Country(countryCode)), Map.of());
  }

  public static Builder builder(String refId) {
    return new Builder(refId);
  }

  @Override
  public Optional<CountryScope> getCountry() {
    return country;
  }

  @Override
  public String getField(String name) {
    return fields.get(name);
  }

  public static final class Builder {

    private final String refId;
    private final Map<String, String> fields = new LinkedHashMap<>();

    private Optional<CountryScope> country = Optional.empty();

    private Builder(String refId) {
      this.refId = refId;
    }

    public Builder inCountry(String countryCode) {
      this.country = Optional.of(new CountryScope.Country(countryCode));
      return this;
    }

    public Builder withField(String name, String value) {
      fields.put(name, value);
      return this;
    }

    public PartnerRecord build() {
      return new TestRecord(refId, country, Map.copyOf(fields));
    }
  }
}
