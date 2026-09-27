package pl.pajak.rule.engine.testing;

import java.util.List;
import pl.pajak.rule.engine.rule.Rule;
import pl.pajak.rule.engine.rule.RuleCatalog;

/** A catalog over a fixed list of rules that counts how often it was loaded. */
public final class FakeRuleCatalog implements RuleCatalog {

  private final List<Rule> rules;

  private int loadCount = 0;

  public FakeRuleCatalog(List<Rule> rules) {
    this.rules = List.copyOf(rules);
  }

  public static FakeRuleCatalog of(Rule... rules) {
    return new FakeRuleCatalog(List.of(rules));
  }

  @Override
  public List<Rule> load() {
    loadCount++;
    return rules;
  }

  public int loadCount() {
    return loadCount;
  }
}
