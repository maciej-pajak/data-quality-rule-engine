package pl.pajak.dqr.example.httpapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pl.pajak.dqr.engine.Decision;
import pl.pajak.dqr.engine.rule.RuleSeverity;
import pl.pajak.dqr.example.httpapi.ValidationResponse.Failure;
import pl.pajak.dqr.example.httpapi.ValidationResponse.Provenance;
import pl.pajak.dqr.example.httpapi.ValidationResponse.Result;

/**
 * The fixture batch of the task description, posted to the running host.
 *
 * <p>The whole stack takes part: the real engine over the example catalog, the records read off a
 * real request body and the outcomes read back off a real response. Only the port is a test detail.
 *
 * <p>What the run decided is asserted through {@link ValidationResponse}, so a decision is compared
 * as a {@link Decision} rather than as the text of one. What the response looks like on the wire is
 * a separate concern, and {@link ValidationApiWireContractTest} is what holds it: deserialising
 * into the host's own records cannot notice that a property was renamed, because the same records
 * would have written the new name.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ValidationApiTest {

  private static final String COUNTRY_BLOCKED = "countryBlocked";
  private static final String COUNTRY_BLOCKED_LABEL = "Is country blocked.";
  private static final String VAT_FORMAT = "vatFormat";
  private static final String VAT_FORMAT_LABEL = "Has a VAT id of plausible length.";
  private static final String IBAN_FORMAT = "ibanFormat";
  private static final String IBAN_FORMAT_LABEL =
      "Has an IBAN that starts with the record's country.";

  @Autowired private TestRestTemplate rest;

  /**
   * Every result is compared whole, field by field: the rule that decided, what it decided, and the
   * provenance that explains it — the computed value, the mapping's word on it and the fields the
   * rule read, which are the reasons the task's table names in parentheses.
   */
  @Test
  void decidesTheFixtureBatch() {
    final var response = postFixtureRecords("/validations");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    final var run = bodyOf(response);
    assertThat(run.results())
        .containsExactlyInAnyOrder(
            result(
                "r1",
                COUNTRY_BLOCKED,
                COUNTRY_BLOCKED_LABEL,
                Decision.NOT_APPLICABLE,
                "ok",
                defaulted(Decision.NOT_APPLICABLE),
                fields("country", "DE")),
            result(
                "r1",
                VAT_FORMAT,
                VAT_FORMAT_LABEL,
                Decision.VALID,
                "ok",
                mapped(Decision.VALID),
                fields("vatId", "DE111111111")),
            result(
                "r1",
                IBAN_FORMAT,
                IBAN_FORMAT_LABEL,
                Decision.VALID,
                "ok",
                mapped(Decision.VALID),
                fields("iban", "DE123456789", "country", "DE")),
            result(
                "r2",
                COUNTRY_BLOCKED,
                COUNTRY_BLOCKED_LABEL,
                Decision.NOT_APPLICABLE,
                "ok",
                defaulted(Decision.NOT_APPLICABLE),
                fields("country", "FR")),
            // r2 is INVALID because its vatId is 4 characters long.
            result(
                "r2",
                VAT_FORMAT,
                VAT_FORMAT_LABEL,
                Decision.INVALID,
                "bad",
                mapped(Decision.INVALID),
                fields("vatId", "FR22")),
            result(
                "r2",
                IBAN_FORMAT,
                IBAN_FORMAT_LABEL,
                Decision.VALID,
                "ok",
                mapped(Decision.VALID),
                fields("iban", "FR123456789", "country", "FR")),
            // r3 is INVALID because its country is the blocked one.
            result(
                "r3",
                COUNTRY_BLOCKED,
                COUNTRY_BLOCKED_LABEL,
                Decision.INVALID,
                "blocked",
                mapped(Decision.INVALID),
                fields("country", "ZZ")),
            // r3 is INVALID because it carries no vatId at all.
            result(
                "r3",
                VAT_FORMAT,
                VAT_FORMAT_LABEL,
                Decision.INVALID,
                "bad",
                mapped(Decision.INVALID),
                fields("vatId", null)),
            // r3 is INVALID because its iban is empty.
            result(
                "r3",
                IBAN_FORMAT,
                IBAN_FORMAT_LABEL,
                Decision.INVALID,
                "bad",
                mapped(Decision.INVALID),
                fields("iban", "", "country", "ZZ")));

    assertThat(run.errors()).isEmpty();
    assertThat(run.summary())
        .isEqualTo(
            new ValidationResponse.Summary(
                9,
                0,
                Map.of(Decision.VALID, 3, Decision.INVALID, 4, Decision.NOT_APPLICABLE, 2),
                Map.of(RuleSeverity.WARNING, 9)));
  }

  @Test
  void reportsAFailingRuleAndFinishesTheBatch() {
    final var response = postFixtureRecords("/validations?demoFailingRule=true");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    final var run = bodyOf(response);
    assertThat(run.errors())
        .extracting(Failure::recordRefId, Failure::ruleId, Failure::cause)
        .containsExactly(tuple("r3", "vatChecksum", NullPointerException.class.getName()));
    assertThat(run.errors()).singleElement().extracting(Failure::message).asString().isNotEmpty();

    // The failing rule still decided the two records that carry a vatId, and every other rule
    // decided all three, so the failure cost the run exactly one evaluation.
    assertThat(run.results()).hasSize(11);
    assertThat(run.summary().resultsCount()).isEqualTo(11);
    assertThat(run.summary().errorsCount()).isEqualTo(1);
  }

  @Test
  void runsOnlyTheRulesTheFiltersSelect() {
    // The example catalog declares no categories, and its rules are all RELEASED.
    assertThat(bodyOf(postFixtureRecords("/validations?category=tax")).results()).isEmpty();
    assertThat(bodyOf(postFixtureRecords("/validations?status=DRAFT")).results()).isEmpty();
  }

  @Test
  void acceptsABatchOfExactlyTheCap() {
    final var response =
        postRecords("/validations", TestRecords.validBatch(ValidationController.MAX_RECORDS));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // Three rules apply to every record, and none of these records is scoped out.
    assertThat(bodyOf(response).summary().resultsCount())
        .isEqualTo(3 * ValidationController.MAX_RECORDS);
  }

  /**
   * The failures a caller can provoke are answered as a {@code ProblemDetail}, which is the wire's
   * own shape rather than the host's, so these are asserted as text.
   */
  @Test
  void rejectsABatchOverTheCap() {
    final var response =
        post("/validations", TestRecords.validBatch(ValidationController.MAX_RECORDS + 1));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("at most " + ValidationController.MAX_RECORDS);
  }

  @Test
  void rejectsARecordWithoutAnId() {
    final var response = post("/validations", "[{\"country\": \"DE\"}]");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("carries no id");
  }

  @Test
  void rejectsABodyThatIsNotAnArrayOfRecords() {
    assertThat(post("/validations", "{}").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(post("/validations", "not json at all").getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private ResponseEntity<ValidationResponse> postFixtureRecords(String path) {
    return postRecords(path, TestRecords.fixtureBatch());
  }

  private ResponseEntity<ValidationResponse> postRecords(String path, String body) {
    return rest.postForEntity(path, jsonRequest(body), ValidationResponse.class);
  }

  /** For the cases whose answer is not a run, so it cannot be read as one. */
  private ResponseEntity<String> post(String path, String body) {
    return rest.postForEntity(path, jsonRequest(body), String.class);
  }

  private static HttpEntity<String> jsonRequest(String body) {
    final var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }

  private static ValidationResponse bodyOf(ResponseEntity<ValidationResponse> response) {
    final var body = response.getBody();
    assertThat(body).as("the response carries a run").isNotNull();
    return body;
  }

  /** One expected result; every rule of the example catalog is a {@link RuleSeverity#WARNING}. */
  private static Result result(
      String recordRefId,
      String ruleId,
      String ruleLabel,
      Decision decision,
      String computedValue,
      String provenanceDescription,
      Map<String, String> accessedFields) {
    return new Result(
        recordRefId,
        ruleId,
        ruleLabel,
        decision,
        RuleSeverity.WARNING,
        new Provenance(computedValue, provenanceDescription, accessedFields));
  }

  /** How the mapping explains a value it lists. */
  private static String mapped(Decision decision) {
    return "the computed value is mapped to " + decision;
  }

  /** How the mapping explains a value it does not list, so its default decided. */
  private static String defaulted(Decision decision) {
    return "the computed value is not mapped, so the default " + decision + " applies";
  }

  /** The fields a rule read. Unlike {@link Map#of}, a field the record does not carry is kept. */
  private static Map<String, String> fields(String name, String value) {
    final var read = new LinkedHashMap<String, String>();
    read.put(name, value);
    return read;
  }

  private static Map<String, String> fields(
      String name, String value, String otherName, String otherValue) {
    final var read = fields(name, value);
    read.put(otherName, otherValue);
    return read;
  }
}
