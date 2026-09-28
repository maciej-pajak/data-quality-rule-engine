package pl.pajak.dqr.example.httpapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * What the response looks like on the wire, asserted as text on purpose.
 *
 * <p>{@link ValidationApiTest} reads the response back into {@link ValidationResponse}, which is
 * the right way to assert what a run decided but blind to the one thing a caller depends on:
 * renaming a record component renames the JSON property, and a test that deserialises with the same
 * records stays green while every client breaks. So the property names live here, written out by
 * hand.
 *
 * <p>The names are asserted in order, because the order a record declares its components is the
 * order they are written in. That is stricter than a client needs, and deliberately so: a
 * reordering is a diff worth seeing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ValidationApiWireContractTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Autowired private TestRestTemplate rest;

  @Test
  void answersResultsErrorsAndSummary() {
    assertThat(propertiesOf(run())).containsExactly("results", "errors", "summary");
  }

  @Test
  void keepsThePropertiesOfAResult() {
    final var result = run().get("results").get(0);

    assertThat(propertiesOf(result))
        .containsExactly(
            "recordRefId", "ruleId", "ruleLabel", "decision", "severity", "provenance");
    assertThat(propertiesOf(result.get("provenance")))
        .containsExactly("computedValue", "description", "accessedFields");
  }

  @Test
  void keepsThePropertiesOfAnError() {
    assertThat(propertiesOf(run().get("errors").get(0)))
        .containsExactly("recordRefId", "ruleId", "ruleLabel", "message", "cause");
  }

  @Test
  void keepsThePropertiesOfTheSummary() {
    assertThat(propertiesOf(run().get("summary")))
        .containsExactly(
            "resultsCount", "errorsCount", "resultsCountByDecision", "resultsCountBySeverity");
  }

  /** The enums travel as their names, and the summary's maps are keyed by those names. */
  @Test
  void writesDecisionsAndSeveritiesByName() {
    final var run = run();

    assertThat(run.get("results").get(0).get("decision").asText())
        .isIn("VALID", "INVALID", "REVIEW", "NOT_APPLICABLE");
    assertThat(run.get("results").get(0).get("severity").asText()).isIn("ERROR", "WARNING", "INFO");
    assertThat(propertiesOf(run.get("summary").get("resultsCountByDecision")))
        .contains("VALID", "INVALID", "NOT_APPLICABLE");
    assertThat(propertiesOf(run.get("summary").get("resultsCountBySeverity")))
        .contains("WARNING", "ERROR");
  }

  /** A field the record does not carry is written as a JSON {@code null}, not left out. */
  @Test
  void writesAnAbsentFieldAsNull() {
    final var vatFormatOfR3 =
        resultOf("r3", "vatFormat").get("provenance").get("accessedFields").get("vatId");

    assertThat(vatFormatOfR3).isNotNull();
    assertThat(vatFormatOfR3.isNull()).isTrue();
  }

  /** One run with a failing rule in it, so results, errors and summary are all populated. */
  private JsonNode run() {
    return parse(postFixtureRecords());
  }

  private JsonNode resultOf(String recordRefId, String ruleId) {
    for (JsonNode result : run().get("results")) {
      if (result.get("recordRefId").asText().equals(recordRefId)
          && result.get("ruleId").asText().equals(ruleId)) {
        return result;
      }
    }
    throw new AssertionError("No result for " + recordRefId + "/" + ruleId);
  }

  private String postFixtureRecords() {
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return rest.postForEntity(
            "/validations?demoFailingRule=true",
            new HttpEntity<>(TestRecords.fixtureBatch(), headers),
            String.class)
        .getBody();
  }

  private static List<String> propertiesOf(JsonNode node) {
    assertThat(node).as("the node is present").isNotNull();
    return node.properties().stream().map(Map.Entry::getKey).toList();
  }

  private static JsonNode parse(String json) {
    try {
      return JSON.readTree(json);
    } catch (IOException ex) {
      throw new AssertionError("Not a JSON response: " + json, ex);
    }
  }
}
