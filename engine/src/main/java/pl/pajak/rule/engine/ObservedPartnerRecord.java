package pl.pajak.rule.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class ObservedPartnerRecord implements PartnerRecord {

  private final PartnerRecord delegate;
  private final Map<String, String> accessedFields = new LinkedHashMap<>();

  ObservedPartnerRecord(PartnerRecord delegate) {
    this.delegate = delegate;
  }

  @Override
  public String refId() {
    return delegate.refId();
  }

  @Override
  public String getField(String name) {
    String value = delegate.getField(name);
    accessedFields.put(name, value);
    return value;
  }

  /**
   * The fields the rule read, in the order it read them. A field the record does not carry is kept
   * with a {@code null} value: the rule saw its absence, and that is what explains the decision.
   */
  public Map<String, String> getAccessedFields() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(accessedFields));
  }
}
