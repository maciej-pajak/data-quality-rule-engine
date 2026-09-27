package pl.pajak.dqr.engine;

public interface RuleResultSink {

  void onSuccess(RuleResult result);

  void onError(RuleError error);
}
