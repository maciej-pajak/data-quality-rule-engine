package pl.pajak.dqr.engine;

import java.util.Objects;
import pl.pajak.dqr.engine.rule.RuleCatalog;
import pl.pajak.dqr.engine.rule.RuleStatus;

record EngineInputImpl(
    RuleCatalog ruleCatalog,
    PartnerRecordSource recordSource,
    RuleResultSink resultSink,
    int batchSize,
    RuleFilter ruleFilter)
    implements EngineInput {

  static final int DEFAULT_BATCH_SIZE = 100;
  static final RuleFilter DEFAULT_RULE_FILTER = RuleFilter.status(RuleStatus.RELEASED);

  EngineInputImpl {
    Objects.requireNonNull(ruleCatalog, "ruleCatalog cannot be null");
    Objects.requireNonNull(recordSource, "recordSource cannot be null");
    Objects.requireNonNull(resultSink, "resultSink cannot be null");
    Objects.requireNonNull(ruleFilter, "ruleFilter cannot be null");
    if (batchSize < 1) {
      throw new IllegalArgumentException("batchSize must be positive, got " + batchSize);
    }
  }

  static final class Builder implements EngineInput.Builder {

    private final RuleCatalog ruleCatalog;
    private final PartnerRecordSource recordSource;
    private final RuleResultSink resultSink;

    private int batchSize = DEFAULT_BATCH_SIZE;
    private RuleFilter ruleFilter = DEFAULT_RULE_FILTER;

    Builder(RuleCatalog ruleCatalog, PartnerRecordSource recordSource, RuleResultSink resultSink) {
      this.ruleCatalog = ruleCatalog;
      this.recordSource = recordSource;
      this.resultSink = resultSink;
    }

    @Override
    public EngineInput.Builder withBatchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    @Override
    public EngineInput.Builder withRuleFilter(RuleFilter ruleFilter) {
      this.ruleFilter = Objects.requireNonNull(ruleFilter, "ruleFilter cannot be null");
      return this;
    }

    @Override
    public EngineInput build() {
      return new EngineInputImpl(ruleCatalog, recordSource, resultSink, batchSize, ruleFilter);
    }
  }
}
