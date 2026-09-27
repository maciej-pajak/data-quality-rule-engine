package pl.pajak.rule.engine;

import java.util.Optional;
import pl.pajak.rule.engine.rule.CountryScope;

public interface PartnerRecord {

  String refId();

  Optional<CountryScope> getCountry();

  String getField(String name);
}
