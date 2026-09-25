package pl.pajak.rule.engine;

import pl.pajak.rule.engine.rule.Rule;

class RulesRun {
  private final RuleResultSink resultSink;

  public RulesRun(RuleResultSink resultSink) {
    this.resultSink = resultSink;
  }

  public RunSummary getSummary() {
    return null;
  }

  public void onResult(RuleResult result) {
    countResult(result);
    resultSink.onSuccess(result);
  }

  public void onFailure(Rule rule, PartnerRecord record, String message) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private void countResult(RuleResult result) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
