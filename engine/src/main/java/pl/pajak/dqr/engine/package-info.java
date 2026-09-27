/**
 * The engine and the seams a host implements around it.
 *
 * <p>A host embeds the library through {@link RuleEngine}, configured by an {@link EngineInput}.
 * Three parts of that input are the host's to write - {@link PartnerRecordSource} for the records,
 * {@link RuleResultSink} for the outcomes, {@link pl.pajak.dqr.engine.rule.RuleCatalog} for the
 * rules - so nothing here assumes where records come from or where results go.
 *
 * <p>The rules themselves live in {@link pl.pajak.dqr.engine.rule}.
 */
package pl.pajak.dqr.engine;
