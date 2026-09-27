package pl.pajak.dqr.rule.java;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import pl.pajak.dqr.engine.rule.ComputationResult;
import pl.pajak.dqr.engine.rule.DecisionMapping;
import pl.pajak.dqr.engine.rule.Rule;
import pl.pajak.dqr.engine.rule.RuleCatalog;
import pl.pajak.dqr.engine.rule.RuleSeverity;
import pl.pajak.dqr.engine.rule.RuleStatus;
import pl.pajak.dqr.engine.rule.ValueLogic;

public class JavaRuleCatalog implements RuleCatalog {

  private static final String DECISION_MAPPINGS_RESOURCE = "decision-mappings.yaml";

  private static final int MIN_VAT_ID_LENGTH = 9;

  private final DecisionMappings decisionMappings;

  public JavaRuleCatalog() {
    this(DecisionMappings.fromClasspath(DECISION_MAPPINGS_RESOURCE));
  }

  JavaRuleCatalog(DecisionMappings decisionMappings) {
    this.decisionMappings = decisionMappings;
  }

  @Override
  public List<Rule> load() {
    return List.of(countryBlocked(), vatFormat(), ibanFormat());
  }

  private Rule countryBlocked() {
    return rule(
        "countryBlocked",
        "Is country blocked.",
        record -> {
          String fieldValue = record.getField("country");
          return computed(Objects.equals("ZZ", fieldValue) ? "blocked" : "ok");
        });
  }

  private Rule vatFormat() {
    return rule(
        "vatFormat",
        "Has a VAT id of plausible length.",
        record -> {
          String vatId = record.getField("vatId");
          return computed(hasText(vatId) && vatId.length() >= MIN_VAT_ID_LENGTH ? "ok" : "bad");
        });
  }

  private Rule ibanFormat() {
    return rule(
        "ibanFormat",
        "Has an IBAN that starts with the record's country.",
        record -> {
          String iban = record.getField("iban");
          String country = record.getField("country");
          return computed(
              hasText(iban) && hasText(country) && iban.startsWith(country) ? "ok" : "bad");
        });
  }

  private Rule rule(String id, String label, ValueLogic logic) {
    return new JavaRule(
        id,
        label,
        RuleStatus.RELEASED,
        RuleSeverity.WARNING,
        List.of(),
        Optional.empty(),
        logic,
        loadDecisionMapping(id));
  }

  private DecisionMapping loadDecisionMapping(String ruleId) {
    return decisionMappings.of(ruleId);
  }

  private static ComputationResult computed(String value) {
    return new ComputationResult.ComputedValue(value);
  }

  /** A field is present when it carries text; a blank value counts as missing. */
  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
