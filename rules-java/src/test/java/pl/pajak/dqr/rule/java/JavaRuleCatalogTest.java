package pl.pajak.dqr.rule.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.engine.Decision;
import pl.pajak.dqr.engine.PartnerRecord;
import pl.pajak.dqr.engine.rule.ComputationResult;
import pl.pajak.dqr.engine.rule.DecisionMapping;
import pl.pajak.dqr.engine.rule.Rule;

class JavaRuleCatalogTest {

  private final JavaRuleCatalog catalog = new JavaRuleCatalog();

  @Test
  void loadsEveryRuleWithTheMappingDeclaredForItsId() {
    final var rules = catalog.load();

    assertThat(rules)
        .extracting(Rule::id)
        .containsExactly("countryBlocked", "vatFormat", "ibanFormat");
    assertThat(rules).allSatisfy(rule -> assertThat(rule.decisionMapping()).isNotNull());
  }

  @Nested
  class CountryBlocked {

    @Test
    void marksTheBlockedCountryInvalid() {
      assertThat(decisionOf("countryBlocked", Map.of("country", "ZZ"))).isEqualTo(Decision.INVALID);
    }

    @Test
    void leavesAnyOtherCountryNotApplicable() {
      assertThat(decisionOf("countryBlocked", Map.of("country", "PL")))
          .isEqualTo(Decision.NOT_APPLICABLE);
      assertThat(decisionOf("countryBlocked", Map.of())).isEqualTo(Decision.NOT_APPLICABLE);
    }
  }

  @Nested
  class VatFormat {

    @Test
    void acceptsAVatIdOfAtLeastNineCharacters() {
      assertThat(decisionOf("vatFormat", Map.of("vatId", "PL1234567"))).isEqualTo(Decision.VALID);
    }

    @Test
    void rejectsAShorterVatId() {
      assertThat(decisionOf("vatFormat", Map.of("vatId", "PL12345"))).isEqualTo(Decision.INVALID);
    }

    @Test
    void rejectsAMissingOrBlankVatId() {
      assertThat(decisionOf("vatFormat", Map.of())).isEqualTo(Decision.INVALID);
      assertThat(decisionOf("vatFormat", Map.of("vatId", "         "))).isEqualTo(Decision.INVALID);
    }
  }

  @Nested
  class IbanFormat {

    @Test
    void acceptsAnIbanThatStartsWithTheRecordCountry() {
      assertThat(decisionOf("ibanFormat", Map.of("country", "PL", "iban", "PL61109010140000")))
          .isEqualTo(Decision.VALID);
    }

    @Test
    void rejectsAnIbanFromAnotherCountry() {
      assertThat(decisionOf("ibanFormat", Map.of("country", "PL", "iban", "DE89370400440532")))
          .isEqualTo(Decision.INVALID);
    }

    @Test
    void rejectsAMissingIbanOrCountry() {
      assertThat(decisionOf("ibanFormat", Map.of("country", "PL"))).isEqualTo(Decision.INVALID);
      assertThat(decisionOf("ibanFormat", Map.of("iban", "PL61109010140000")))
          .isEqualTo(Decision.INVALID);
    }
  }

  private Decision decisionOf(String ruleId, Map<String, String> fields) {
    final var rule =
        catalog.load().stream().filter(it -> it.id().equals(ruleId)).findFirst().orElseThrow();
    final var computed = rule.logic().compute(new FieldMapRecord(fields));
    final var value =
        switch (computed) {
          case ComputationResult.ComputedValue v -> v.value();
          case ComputationResult.ComputationError e ->
              throw new AssertionError("Unexpected computation error: " + e.message());
        };
    return switch (rule.decisionMapping().decisionOf(value)) {
      case DecisionMapping.Resolution.Matched m -> m.decision();
      case DecisionMapping.Resolution.Defaulted d -> d.decision();
      case DecisionMapping.Resolution.Unmapped u ->
          throw new AssertionError("Value left unmapped by " + ruleId + ": " + u.value());
    };
  }

  private record FieldMapRecord(Map<String, String> fields) implements PartnerRecord {

    @Override
    public String refId() {
      return "test-record";
    }

    @Override
    public String getField(String name) {
      return fields.get(name);
    }
  }
}
