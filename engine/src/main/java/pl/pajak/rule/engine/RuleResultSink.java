package pl.pajak.rule.engine;

public interface RuleResultSink {

  void onSuccess(RuleResult result);

  void onError(RuleError error);
}
