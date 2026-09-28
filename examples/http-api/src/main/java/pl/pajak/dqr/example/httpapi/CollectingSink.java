package pl.pajak.dqr.example.httpapi;

import java.util.ArrayList;
import java.util.List;
import pl.pajak.dqr.engine.RuleError;
import pl.pajak.dqr.engine.RuleResult;
import pl.pajak.dqr.engine.RuleResultSink;

/**
 * A sink that keeps the outcomes of a run so they can be answered in one response.
 *
 * <p>Collecting is what an HTTP response of a bounded batch needs, and it is a choice of this host,
 * not of the library: the engine hands over each outcome as it is produced and holds none itself. A
 * host with a million records fills this same seam with a writer to a file, a topic or a collection
 * and keeps nothing.
 *
 * <p>The engine calls a sink from the thread that started the run, one call at a time, so the lists
 * need no synchronisation.
 */
final class CollectingSink implements RuleResultSink {

  private final List<ValidationResponse.Result> results = new ArrayList<>();

  private final List<ValidationResponse.Failure> errors = new ArrayList<>();

  @Override
  public void onResult(RuleResult result) {
    results.add(ValidationResponse.Result.of(result));
  }

  @Override
  public void onError(RuleError error) {
    errors.add(ValidationResponse.Failure.of(error));
  }

  List<ValidationResponse.Result> results() {
    return List.copyOf(results);
  }

  List<ValidationResponse.Failure> errors() {
    return List.copyOf(errors);
  }
}
