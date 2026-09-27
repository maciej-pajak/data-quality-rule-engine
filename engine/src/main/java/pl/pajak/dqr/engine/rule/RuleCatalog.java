package pl.pajak.dqr.engine.rule;

import java.util.List;

/**
 * The set of rules a run may evaluate.
 *
 * <p>Where rules come from is the host's business: a catalog may hold them in code, read them from
 * a document or fetch them from a service. It is loaded once per run, and a run may evaluate only a
 * subset of what it returns.
 */
public interface RuleCatalog {

  /**
   * Returns the rules of this catalog.
   *
   * @return the rules, in the order they are to be evaluated; never {@code null}
   * @throws RuntimeException if the rules cannot be loaded; the run ends before any record is read
   */
  List<Rule> load();
}
