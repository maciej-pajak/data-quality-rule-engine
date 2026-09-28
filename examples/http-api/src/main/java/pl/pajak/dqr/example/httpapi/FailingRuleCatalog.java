package pl.pajak.dqr.example.httpapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pl.pajak.dqr.engine.Decision;
import pl.pajak.dqr.engine.rule.ComputationResult;
import pl.pajak.dqr.engine.rule.Rule;
import pl.pajak.dqr.engine.rule.RuleCatalog;
import pl.pajak.dqr.engine.rule.RuleSeverity;
import pl.pajak.dqr.engine.rule.RuleStatus;
import pl.pajak.dqr.example.rules.JavaRule;
import pl.pajak.dqr.example.rules.ValueDecisionMapping;

/**
 * A catalog decorator that appends one rule with a bug in it, so fault isolation can be seen over
 * HTTP.
 *
 * <p>Requested per run with {@code ?demoFailingRule=true}. The rule reads {@code vatId} without
 * guarding against its absence, the way a rule written in a hurry does: the record that carries
 * none makes it throw, that record and rule are reported as one {@code error} line, and every other
 * rule and record of the batch is still decided.
 */
final class FailingRuleCatalog implements RuleCatalog {

  private static final String RULE_ID = "vatChecksum";

  private final RuleCatalog delegate;

  FailingRuleCatalog(RuleCatalog delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<Rule> load() {
    final var rules = new ArrayList<>(delegate.load());
    rules.add(failingRule());
    return List.copyOf(rules);
  }

  private static Rule failingRule() {
    return new JavaRule(
        RULE_ID,
        "Verifies the VAT id against an external checksum service.",
        RuleStatus.RELEASED,
        RuleSeverity.ERROR,
        List.of(),
        Optional.empty(),
        record -> {
          final var vatId = record.getField("vatId");
          // Deliberately unguarded: a record without a vatId makes this rule throw.
          return new ComputationResult.ComputedValue(vatId.isBlank() ? "bad" : "ok");
        },
        new ValueDecisionMapping(
            Map.of("ok", Decision.VALID, "bad", Decision.INVALID), Optional.empty()));
  }
}
