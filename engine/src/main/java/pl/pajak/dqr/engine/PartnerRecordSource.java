package pl.pajak.dqr.engine;

import java.util.List;

public interface PartnerRecordSource {

  List<PartnerRecord> readNextBatch(int maxRecords);
}
