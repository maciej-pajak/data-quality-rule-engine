package pl.pajak.rule.engine;

import java.util.List;

public interface PartnerRecordSource {

  List<PartnerRecord> readNextBatch(int maxRecords);
}
