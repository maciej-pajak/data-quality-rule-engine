package pl.pajak.rule.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import pl.pajak.rule.engine.rule.CountryScope;

class ObservedPartnerRecord implements PartnerRecord {

  private final PartnerRecord delegate;
  private final Map<String, String> accessedFields = new HashMap<>();

  ObservedPartnerRecord(PartnerRecord delegate) {
    this.delegate = delegate;
  }

  @Override
  public String refId() {
    return delegate.refId();
  }

  @Override
  public Optional<CountryScope> getCountry() {
    return delegate.getCountry();
  }

  @Override
  public void read(PartnerRecordVisitor visitor) {
    delegate.read(visitor);
  }

  @Override
  public String getField(String name) {
    String value = delegate.getField(name);
    accessedFields.put(name, value);
    return value;
  }

  public Map<String, String> getAccessedFields() {
    return Map.copyOf(accessedFields);
  }
}
