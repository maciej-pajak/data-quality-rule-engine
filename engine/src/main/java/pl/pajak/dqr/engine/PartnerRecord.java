package pl.pajak.dqr.engine;

/**
 * A single business-partner record supplied to the engine for validation.
 *
 * <p>Records are schemaless: fields are looked up by name, so implementations may carry any set of
 * fields and change it without affecting the engine. Implementations are supplied by the host
 * through a {@link PartnerRecordSource}; the engine only reads them, from a single thread.
 */
public interface PartnerRecord {

  /**
   * Returns the identifier of this record, as assigned by the host.
   *
   * <p>The value is opaque to the engine, which reports it with every outcome of this record rather
   * than parsing it. It should be unique within a run, otherwise results and errors cannot be
   * attributed to a single record.
   *
   * @return the record identifier
   */
  String refId();

  /**
   * Returns the value of a flat text field.
   *
   * <p>The engine reads the {@code "country"} field to match rules scoped to a single country;
   * implementations whose own schema names that field differently should answer it here.
   *
   * @param name the field name
   * @return the field value, or {@code null} if the record does not carry the field
   * @throws RuntimeException if the value cannot be read, for example when the record is backed by
   *     a remote document; the engine reports such a failure as a {@link RuleError} for the record
   *     and rule being evaluated and continues with the rest of the batch
   */
  String getField(String name);
}
