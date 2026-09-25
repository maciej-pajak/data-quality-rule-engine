package pl.pajak.rule.engine.rule;

public sealed interface CountryScope permits CountryScope.World, CountryScope.Country {

  CountryScope World = new World();

  record World() implements CountryScope {}

  record Country(String code) implements CountryScope {}
}
