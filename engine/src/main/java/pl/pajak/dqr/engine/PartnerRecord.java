package pl.pajak.dqr.engine;

public interface PartnerRecord {

  String refId();

  /**
   * The value of one flat text field, or {@code null} when the record does not carry it.
   *
   * <p>The engine asks for {@code "country"} when it matches a rule scoped to one country against a
   * record, so a record whose own schema names that field differently answers it here.
   *
   * <p>An implementation may throw: a record backed by a remote document can fail while it is read.
   * The engine contains such a failure as an error reported for that record and rule, so the rest
   * of the batch still runs.
   */
  String getField(String name);
}
