package pl.pajak.dqr.example.httpapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.pajak.dqr.engine.RuleEngine;
import pl.pajak.dqr.engine.rule.RuleCatalog;
import pl.pajak.dqr.example.rules.JavaRuleCatalog;

/**
 * The two seams this host fills once, at startup.
 *
 * <p>The engine holds no state between runs, so one instance serves every request. The catalog is
 * built once here rather than per request: it reads its decision mappings when it is created, so a
 * malformed mapping fails the context at startup instead of the first caller.
 */
@Configuration(proxyBeanMethods = false)
class EngineConfiguration {

  @Bean
  RuleEngine ruleEngine() {
    return RuleEngine.newInstance();
  }

  @Bean
  RuleCatalog ruleCatalog() {
    return new JavaRuleCatalog();
  }
}
