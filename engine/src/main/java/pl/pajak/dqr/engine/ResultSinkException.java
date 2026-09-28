package pl.pajak.dqr.engine;

/**
 * Thrown when a {@link RuleResultSink} fails to accept an outcome, ending the run.
 *
 * <p>Outcomes delivered before the failure stand; no {@link RunSummary} is produced. The rule and
 * record being delivered are carried along, so a host can resume near where the run stopped.
 */
public final class ResultSinkException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String recordRefId;

  private final String ruleId;

  ResultSinkException(String recordRefId, String ruleId, String what, Throwable cause) {
    super(
        "The result sink failed to accept "
            + what
            + " of rule "
            + ruleId
            + " on record "
            + recordRefId,
        cause);
    this.recordRefId = recordRefId;
    this.ruleId = ruleId;
  }

  /**
   * Returns the record whose outcome the sink refused.
   *
   * @return the reference of the record the run stopped on
   */
  public String recordRefId() {
    return recordRefId;
  }

  /**
   * Returns the rule whose outcome the sink refused.
   *
   * @return the id of the rule the run stopped on
   */
  public String ruleId() {
    return ruleId;
  }
}
