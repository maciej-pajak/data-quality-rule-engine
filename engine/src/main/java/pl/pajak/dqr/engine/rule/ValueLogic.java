package pl.pajak.dqr.engine.rule;

import pl.pajak.dqr.engine.PartnerRecord;

public interface ValueLogic {

  ComputationResult compute(PartnerRecord partnerRecord);
}
