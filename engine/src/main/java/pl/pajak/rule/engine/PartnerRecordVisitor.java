package pl.pajak.rule.engine;

import java.math.BigDecimal;

public interface PartnerRecordVisitor {
  void text(String name, String value);

  void number(String name, BigDecimal value);

  void bool(String name, boolean value);

  void nullValue(String name);

  void startObject(String name);

  void endObject();

  void startList(String name);

  void endList();
}
