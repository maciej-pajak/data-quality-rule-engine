package pl.pajak.dqr.example.httpapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * An example host: an HTTP API that embeds the rule engine.
 *
 * <p>Nothing here is part of the product. It shows what a host has to supply — a {@link
 * pl.pajak.dqr.engine.rule.RuleCatalog}, a {@link pl.pajak.dqr.engine.PartnerRecordSource} and a
 * {@link pl.pajak.dqr.engine.RuleResultSink} — and that the library can be embedded without holding
 * a batch, a request or a response in memory.
 */
@SpringBootApplication
public class HttpApiApplication {

  /**
   * Starts the host.
   *
   * @param args the Spring Boot command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(HttpApiApplication.class, args);
  }
}
