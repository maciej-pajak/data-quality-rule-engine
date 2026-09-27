package pl.pajak.dqr.engine.testing;

import java.util.ArrayList;
import java.util.List;
import pl.pajak.dqr.engine.RuleError;
import pl.pajak.dqr.engine.RuleResult;
import pl.pajak.dqr.engine.RuleResultSink;

/** A sink that keeps everything it was handed, in the order it arrived. */
public class RecordingSink implements RuleResultSink {

  private final List<RuleResult> results = new ArrayList<>();
  private final List<RuleError> errors = new ArrayList<>();

  @Override
  public void onResult(RuleResult result) {
    results.add(result);
  }

  @Override
  public void onError(RuleError error) {
    errors.add(error);
  }

  public List<RuleResult> results() {
    return List.copyOf(results);
  }

  public List<RuleError> errors() {
    return List.copyOf(errors);
  }
}
