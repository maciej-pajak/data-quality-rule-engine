package pl.pajak.rule.engine;

import java.util.List;
import java.util.function.Predicate;
import pl.pajak.rule.engine.rule.ComputationResult;
import pl.pajak.rule.engine.rule.CountryScope;
import pl.pajak.rule.engine.rule.DecisionMapping;
import pl.pajak.rule.engine.rule.Rule;

public class RuleEngine {

  public RuleEngine() {}

  public RunSummary process(EngineInput engineInput) {
    final var input =
        switch (engineInput) {
          case EngineInputImpl impl -> impl;
        };
    final var recordSource = input.recordSource();
    final var rules = loadRules(input);

    final var run = new RulesRun(input.resultSink());

    List<PartnerRecord> batch;
    do {
      // F1: The engine reads a batch of records
      batch = recordSource.readNextBatch(input.batchSize());

      evaluate(batch, rules, run);
    } while (!batch.isEmpty());

    return run.getSummary();
  }

  private List<Rule> loadRules(EngineInputImpl input) {
    final var ruleCatalog = input.ruleCatalog();
    return ruleCatalog.load().stream()
        .filter(r -> RuleMatcher.matches(input.ruleFilter(), r))
        .toList();
  }

  private void evaluate(List<PartnerRecord> batch, List<Rule> rules, RulesRun validationRun) {

    for (Rule rule : rules) {
      batch.stream()
          .filter(matchesCountry(rule))
          .forEach(
              partnerRecord -> {
                try {
                  ObservedPartnerRecord observedRecord = new ObservedPartnerRecord(partnerRecord);
                  ComputationResult result = rule.logic().compute(observedRecord);

                  switch (result) {
                    case ComputationResult.ComputedValue value ->
                        decide(rule, observedRecord, value, validationRun);
                    case ComputationResult.ComputationError error ->
                        validationRun.onFailure(rule, partnerRecord, error);
                  }
                } catch (Exception ex) {
                  validationRun.onFailure(rule, partnerRecord, ex);
                }
              });
    }
  }

  private Predicate<PartnerRecord> matchesCountry(Rule rule) {
    final var countryScope = rule.countryScope().orElse(CountryScope.World);
    return record ->
        countryScope.equals(CountryScope.World) || countryScope.equals(record.getCountry().orElse(null));
  }

  /**
   * Turns a computed value into a result, or into an error when the rule's mapping covers neither
   * the value nor a default. An unmapped value depends on the record, so it is reported per record
   * rather than aborting the run.
   */
  private void decide(
      Rule rule,
      ObservedPartnerRecord partnerRecord,
      ComputationResult.ComputedValue value,
      RulesRun validationRun) {
    switch (rule.decisionMapping().decisionOf(value.value())) {
      case DecisionMapping.Resolution.Resolved resolved ->
          validationRun.onResult(resultOf(rule, partnerRecord, value, resolved));
      case DecisionMapping.Resolution.Unmapped unmapped ->
          validationRun.onFailure(
              rule,
              partnerRecord,
              "computed value \""
                  + unmapped.value()
                  + "\" is not mapped and the rule declares no default decision");
    }
  }

  private RuleResult resultOf(
      Rule rule,
      ObservedPartnerRecord partnerRecord,
      ComputationResult.ComputedValue value,
      DecisionMapping.Resolution.Resolved resolved) {
    return new RuleResultImpl(
        partnerRecord.refId(),
        rule.id(),
        resolved.decision(),
        rule.severity(),
        new RuleResultImpl.ProvenanceImpl(
            value.value(), resolved.description(), partnerRecord.getAccessedFields()));
  }
}
