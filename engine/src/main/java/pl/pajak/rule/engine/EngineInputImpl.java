package pl.pajak.rule.engine;

import java.util.Objects;
import pl.pajak.rule.engine.rule.RuleCatalog;

record EngineInputImpl(
    RuleCatalog ruleCatalog,
    PartnerRecordSource recordSource,
    RuleResultSink resultSink,
    int batchSize,
    RuleFilter ruleFilter)
    implements EngineInput {

  EngineInputImpl {
    Objects.requireNonNull(ruleCatalog, "ruleCatalog cannot be null");
    Objects.requireNonNull(recordSource, "recordSource cannot be null");
    Objects.requireNonNull(resultSink, "resultSink cannot be null");
  }

  static final class Builder implements EngineInput.Builder {

    Builder(RuleCatalog ruleCatalog, PartnerRecordSource recordSource, RuleResultSink resultSink) {}

    @Override
    public EngineInput.Builder withBatchSize(int batchSize) {
      return null;
    }

    @Override
    public EngineInput.Builder withRuleFilter(RuleFilter ruleFilter) {
      return null;
    }

    @Override
    public EngineInput build() {
      return null;
    }
  }
}
