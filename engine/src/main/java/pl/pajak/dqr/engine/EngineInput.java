package pl.pajak.dqr.engine;

import pl.pajak.dqr.engine.rule.RuleCatalog;

/**
 * Configuration of a single engine run: the rules to evaluate, the records to evaluate them
 * against, and the destination for the results.
 *
 * <p>The rule catalog, the record source and the result sink are required and are passed to {@link
 * #builder(RuleCatalog, PartnerRecordSource, RuleResultSink)}. The batch size and the rule filter
 * are optional and have defaults.
 *
 * <p>Instances are immutable and can be passed to the engine more than once, provided the record
 * source and the result sink support it.
 *
 * <pre>{@code
 * EngineInput input = EngineInput.builder(catalog, source, sink)
 *     .withBatchSize(500)
 *     .withRuleFilter(RuleFilter.category("address"))
 *     .build();
 * }</pre>
 *
 * @see RuleEngineImpl#process(EngineInput)
 */
public sealed interface EngineInput permits EngineInputImpl {

  /**
   * Creates a builder for a run over the given catalog, source and sink.
   *
   * @param ruleCatalog the rules to evaluate; loaded once per run
   * @param recordSource the records to evaluate; read in batches until it returns none
   * @param resultSink receives results and errors while the run proceeds
   * @return a builder using the default batch size and rule filter
   */
  static Builder builder(
      RuleCatalog ruleCatalog, PartnerRecordSource recordSource, RuleResultSink resultSink) {
    return new EngineInputImpl.Builder(ruleCatalog, recordSource, resultSink);
  }

  /** Builder for the optional settings of a run. */
  sealed interface Builder permits EngineInputImpl.Builder {

    /**
     * Sets how many records the engine requests from the source at a time. Defaults to 100.
     *
     * @param batchSize the maximum number of records per read; must be positive
     * @return this builder
     */
    Builder withBatchSize(int batchSize);

    /**
     * Sets which rules of the catalog take part in the run. Defaults to RELEASED rules only.
     *
     * @param ruleFilter the filter applied to the loaded catalog; must not be {@code null}
     * @return this builder
     */
    Builder withRuleFilter(RuleFilter ruleFilter);

    /**
     * Builds the configuration.
     *
     * @return an immutable input
     * @throws NullPointerException if the catalog, the source, the sink or the filter is {@code
     *     null}
     * @throws IllegalArgumentException if the batch size is not positive
     */
    EngineInput build();
  }
}
