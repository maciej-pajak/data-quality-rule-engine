package pl.pajak.rule.engine.rule;

import pl.pajak.rule.engine.PartnerRecord;

public interface ValueLogic {

  ComputationResult compute(PartnerRecord partnerRecord);
}
