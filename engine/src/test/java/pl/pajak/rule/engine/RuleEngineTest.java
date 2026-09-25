package pl.pajak.rule.engine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pl.pajak.rule.engine.rule.RuleStatus;

class RuleEngineTest {

  @Test
  @Disabled("work in progress")
  void testApi() {
    RuleEngine ruleEngine = new RuleEngine();

    EngineInput input =
        EngineInput.builder(null, null, null)
            .withRuleFilter(
                RuleFilter.and(
                    RuleFilter.or(RuleFilter.category("a"), RuleFilter.category("b")),
                    RuleFilter.not(RuleFilter.status(RuleStatus.DEPRECATED))))
            .withBatchSize(1000)
            .build();

    RunSummary summary = ruleEngine.process(input);
  }
}
