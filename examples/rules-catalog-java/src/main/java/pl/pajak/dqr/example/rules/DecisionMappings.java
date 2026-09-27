package pl.pajak.dqr.example.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import pl.pajak.dqr.engine.Decision;
import pl.pajak.dqr.engine.rule.DecisionMapping;

/**
 * Decision mappings read from a YAML document, keyed by rule id.
 *
 * <p>Expected shape:
 *
 * <pre>
 * decision-mappings:
 *   countryBlocked:
 *     decision-by-value:
 *       blocked: INVALID
 *     default-decision: NOT_APPLICABLE
 * </pre>
 *
 * <p>The document is bound to records once, when this object is created, so a malformed mapping —
 * an unknown key, a misspelled decision — fails at catalog construction rather than mid-run.
 */
final class DecisionMappings {

  /** Kebab-case keys bind to camelCase components, the way Spring's relaxed binding does. */
  private static final ObjectMapper YAML =
      new ObjectMapper(new YAMLFactory())
          .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

  private final Map<String, DecisionMapping> byRuleId;

  private DecisionMappings(Map<String, DecisionMapping> byRuleId) {
    this.byRuleId = Map.copyOf(byRuleId);
  }

  /** Reads the mappings from a classpath resource. */
  static DecisionMappings fromClasspath(String resource) {
    try (InputStream source =
        DecisionMappings.class.getClassLoader().getResourceAsStream(resource)) {
      if (source == null) {
        throw new IllegalStateException("Decision mappings resource not found: " + resource);
      }
      return parse(source);
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot read decision mappings from " + resource, ex);
    }
  }

  static DecisionMappings parse(InputStream source) {
    final Document document;
    try {
      document = YAML.readValue(source, Document.class);
    } catch (IOException ex) {
      throw new IllegalStateException("Malformed decision mappings: " + ex.getMessage(), ex);
    }
    if (document == null) {
      throw new IllegalStateException("Decision mappings document is empty");
    }

    final Map<String, DecisionMapping> byRuleId = new LinkedHashMap<>();
    document.decisionMappings().forEach((ruleId, spec) -> byRuleId.put(ruleId, spec.toMapping()));
    return new DecisionMappings(byRuleId);
  }

  /** The mapping declared for {@code ruleId}; a rule without one is a configuration error. */
  DecisionMapping of(String ruleId) {
    final var mapping = byRuleId.get(ruleId);
    if (mapping == null) {
      throw new IllegalStateException("No decision mapping declared for rule: " + ruleId);
    }
    return mapping;
  }

  /** The document's root. Unknown keys are rejected, so a typo is reported instead of ignored. */
  private record Document(Map<String, Spec> decisionMappings) {

    Document {
      Objects.requireNonNull(decisionMappings, "decision-mappings is missing");
    }
  }

  /** One rule's mapping as written in the document. */
  private record Spec(Map<String, Decision> decisionByValue, Decision defaultDecision) {

    Spec {
      Objects.requireNonNull(decisionByValue, "decision-by-value is missing");
    }

    DecisionMapping toMapping() {
      return new ValueDecisionMapping(decisionByValue, Optional.ofNullable(defaultDecision));
    }
  }
}
