package pl.pajak.dqr.engine;

import java.util.List;
import pl.pajak.dqr.engine.rule.ComputationResult;
import pl.pajak.dqr.engine.rule.CountryScope;
import pl.pajak.dqr.engine.rule.DecisionMapping;
import pl.pajak.dqr.engine.rule.Rule;

final class RuleEngineImpl implements RuleEngine {

  /** The record field the engine reads to match a rule scoped to one country. */
  private static final String COUNTRY_FIELD = "country";

  RuleEngineImpl() {}

  @Override
  public RunSummary process(EngineInput engineInput) {
    final var input =
        switch (engineInput) {
          case EngineInputImpl impl -> impl;
        };
    final var recordSource = input.recordSource();
    final var rules = loadRules(input);

    final var run = new RulesRun(input.resultSink());

    for (List<PartnerRecord> batch = recordSource.readNextBatch(input.batchSize());
        !batch.isEmpty();
        batch = recordSource.readNextBatch(input.batchSize())) {
      evaluate(batch, rules, run);
    }

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
      for (PartnerRecord partnerRecord : batch) {
        try {
          if (!appliesTo(rule, partnerRecord)) {
            continue;
          }

          ObservedPartnerRecord observedRecord = new ObservedPartnerRecord(partnerRecord);
          ComputationResult result = rule.logic().compute(observedRecord);

          switch (result) {
            case ComputationResult.ComputedValue value ->
                decide(rule, observedRecord, value, validationRun);
            case ComputationResult.ComputationError error ->
                validationRun.onFailure(rule, partnerRecord, error);
          }
        } catch (ResultSinkException ex) {
          // The destination of the run failed, not the rule. There is nowhere left to report to.
          throw ex;
        } catch (Exception ex) {
          validationRun.onFailure(rule, partnerRecord, ex);
        }
      }
    }
  }

  /**
   * Whether a rule's country scope covers a record. The record's country is an ordinary field, so a
   * record that carries none is only covered by a world wide rule.
   */
  private boolean appliesTo(Rule rule, PartnerRecord record) {
    return switch (rule.countryScope().orElse(CountryScope.World)) {
      case CountryScope.World ignored -> true;
      case CountryScope.Country(String code) -> code.equals(record.getField(COUNTRY_FIELD));
    };
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
        rule.label(),
        resolved.decision(),
        rule.severity(),
        new RuleResultImpl.ProvenanceImpl(
            value.value(), resolved.description(), partnerRecord.getAccessedFields()));
  }
}
