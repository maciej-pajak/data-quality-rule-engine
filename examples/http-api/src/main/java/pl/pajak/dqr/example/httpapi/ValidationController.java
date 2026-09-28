package pl.pajak.dqr.example.httpapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pajak.dqr.engine.EngineInput;
import pl.pajak.dqr.engine.RuleEngine;
import pl.pajak.dqr.engine.RuleFilter;
import pl.pajak.dqr.engine.rule.RuleCatalog;
import pl.pajak.dqr.engine.rule.RuleStatus;

/**
 * Validates a batch of posted records and answers the results in one response.
 *
 * <pre>{@code
 * curl -s localhost:8080/validations \
 *   -H 'Content-Type: application/json' --data-binary @records.json
 * }</pre>
 *
 * <p>The endpoint is deliberately the small, synchronous one: a caller posts a bounded batch, the
 * engine runs it on the request thread, and everything it produced is answered together. That is
 * what an interactive caller wants, and it keeps the example about embedding the library rather
 * than about HTTP.
 *
 * <p>Which is why the batch is capped at {@value #MAX_RECORDS} records. A run of a million records
 * does not belong in a request: it takes minutes, it outlives client and proxy timeouts, and a
 * connection that drops halfway leaves the caller with no way to resume. Such a host submits a job
 * over the data where it rests and streams the outcomes to a sink that is not a socket; it fills
 * the same seams, so it is a different host rather than a different library. See {@code DESIGN.md}.
 */
@RestController
class ValidationController {

  /** The largest batch this endpoint accepts; a bigger one is a job, not a request. */
  static final int MAX_RECORDS = 100;

  private final RuleEngine ruleEngine;

  private final RuleCatalog ruleCatalog;

  ValidationController(RuleEngine ruleEngine, RuleCatalog ruleCatalog) {
    this.ruleEngine = ruleEngine;
    this.ruleCatalog = ruleCatalog;
  }

  /**
   * Runs the selected rules over the posted records.
   *
   * @param records a JSON array of flat record objects, each with an {@code id}; at most {@value
   *     #MAX_RECORDS}
   * @param status the rule status to select; defaults to {@code RELEASED}
   * @param categories the categories to select, any of which matches; defaults to every category
   * @param demoFailingRule appends a rule that throws on a record without a {@code vatId}
   * @return the results, the rules that failed, and the summary of the run
   * @throws IllegalArgumentException if the batch is too large or a record carries no {@code id}
   */
  @Operation(
      summary = "Validate a batch of business-partner records",
      description =
          """
          Answers a `result` per rule and record, an `error` for every rule that failed without \
          stopping the run, and the run's `summary`.""")
  @ApiResponse(responseCode = "200", description = "The outcomes of the run")
  @ApiResponse(responseCode = "400", description = "The batch is malformed or too large")
  @PostMapping(
      path = "/validations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ValidationResponse validate(
      @RequestBody List<Map<String, String>> records,
      @RequestParam(defaultValue = "RELEASED") RuleStatus status,
      @RequestParam(name = "category", required = false) List<String> categories,
      @RequestParam(defaultValue = "false") boolean demoFailingRule) {

    if (records.size() > MAX_RECORDS) {
      throw new IllegalArgumentException(
          "A batch holds at most " + MAX_RECORDS + " records, got " + records.size());
    }

    final var catalog = demoFailingRule ? new FailingRuleCatalog(ruleCatalog) : ruleCatalog;
    final var sink = new CollectingSink();
    final var summary =
        ruleEngine.process(
            EngineInput.builder(catalog, PostedRecords.sourceOf(records), sink)
                .withRuleFilter(ruleFilter(status, categories))
                .build());

    return new ValidationResponse(
        sink.results(), sink.errors(), ValidationResponse.Summary.of(summary));
  }

  /**
   * The filter of one run: the status the caller asked for, narrowed by the categories it named.
   *
   * <p>Composed from the engine's own filters, so the host contributes no matching logic of its
   * own.
   */
  private static RuleFilter ruleFilter(RuleStatus status, List<String> categories) {
    final var onStatus = RuleFilter.status(status);
    if (categories == null || categories.isEmpty()) {
      return onStatus;
    }
    return onStatus.and(anyCategoryOf(categories));
  }

  private static RuleFilter anyCategoryOf(List<String> categories) {
    if (categories.size() == 1) {
      return RuleFilter.category(categories.getFirst());
    }
    return RuleFilter.anyOf(
        categories.stream().map(RuleFilter::category).toArray(RuleFilter[]::new));
  }
}
