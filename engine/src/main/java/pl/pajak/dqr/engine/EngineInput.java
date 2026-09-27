package pl.pajak.dqr.engine;

import pl.pajak.dqr.engine.rule.RuleCatalog;

public sealed interface EngineInput permits EngineInputImpl {

  static Builder builder(
      RuleCatalog ruleCatalog, PartnerRecordSource recordSource, RuleResultSink resultSink) {
    return new EngineInputImpl.Builder(ruleCatalog, recordSource, resultSink);
  }

  sealed interface Builder permits EngineInputImpl.Builder {

    Builder withBatchSize(int batchSize);

    Builder withRuleFilter(RuleFilter ruleFilter);

    EngineInput build();
  }
}
