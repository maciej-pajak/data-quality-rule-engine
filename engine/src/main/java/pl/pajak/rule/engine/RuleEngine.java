package pl.pajak.rule.engine;

import java.util.List;
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
      final var countryScope = rule.countryScope().orElse(CountryScope.World);

      batch.stream()
          .filter(
              record -> countryScope.equals(record.getCountry().orElse(CountryScope.World))) // TODO
          .forEach(
              partnerRecord -> {
                try {
                  ComputationResult result = rule.logic().compute(partnerRecord);

                  switch (result) {
                    case ComputationResult.ComputedValue value ->
                        validationRun.onResult(resultOf(rule, partnerRecord, value));
                    case ComputationResult.ComputationError error ->
                        validationRun.onFailure(rule, partnerRecord, error.message());
                  }
                } catch (Exception ex) {
                  validationRun.onFailure(rule, partnerRecord, ex.getMessage()); // TODO
                }
              });
    }
  }

  private RuleResult resultOf(
      Rule rule, PartnerRecord partnerRecord, ComputationResult.ComputedValue value) {
    final var decision =
        switch (rule.decisionMapping().decisionOf(value.value())) {
          case DecisionMapping.Resolution.Matched matched -> matched.decision();
          case DecisionMapping.Resolution.Defaulted defaulted -> defaulted.decision();
          case DecisionMapping.Resolution.Unmapped unmapped ->
              throw new IllegalStateException(
                  "Unexpected decision mapping resolution: " + unmapped); // TODO
        };
    return new RuleResultImpl(
        partnerRecord.refId(),
        rule.id(),
        decision,
        rule.severity(),
        new RuleResultImpl.ProvenanceImpl(
            value.value(),
            null, // TODO
            null));
  }
}
