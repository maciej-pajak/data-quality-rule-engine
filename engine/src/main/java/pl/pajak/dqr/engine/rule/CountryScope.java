package pl.pajak.dqr.engine.rule;

/**
 * The countries a rule applies to.
 *
 * <p>A {@link World} scope covers every record; a {@link Country} scope covers only the records of
 * that country. A rule is evaluated against a record only when its scope covers it.
 */
public sealed interface CountryScope permits CountryScope.World, CountryScope.Country {

  /**
   * The scope of a rule that applies everywhere; the type carries no state, so one value serves.
   */
  CountryScope World = new World();

  /** Applies to every record. Use the shared {@link CountryScope#World} value. */
  record World() implements CountryScope {}

  /**
   * Applies to the records of one country.
   *
   * @param code the country as it is written in the record's {@code country} field; matched exactly
   */
  record Country(String code) implements CountryScope {}
}
