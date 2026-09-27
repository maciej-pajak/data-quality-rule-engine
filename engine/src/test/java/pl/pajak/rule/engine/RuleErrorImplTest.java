package pl.pajak.rule.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class RuleErrorImplTest {

  @Test
  void rejectsAMissingRecordRefId() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RuleErrorImpl(null, "rule-1", "message", null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RuleErrorImpl(" ", "rule-1", "message", null));
  }

  @Test
  void rejectsAMissingRuleId() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RuleErrorImpl("r-1", null, "message", null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RuleErrorImpl("r-1", "", "message", null));
  }

  @Test
  void rejectsABlankMessageSoAnErrorAlwaysDescribesItself() {
    assertThatNullPointerException()
        .isThrownBy(() -> new RuleErrorImpl("r-1", "rule-1", null, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RuleErrorImpl("r-1", "rule-1", "\t", null));
  }

  @Test
  void reportsAnAbsentCauseAsAnEmptyOptional() {
    assertThat(new RuleErrorImpl("r-1", "rule-1", "message", null).cause()).isEmpty();
  }

  @Test
  void reportsThePresentCause() {
    final var cause = new IllegalStateException("boom");

    assertThat(new RuleErrorImpl("r-1", "rule-1", "message", cause).cause()).containsSame(cause);
  }
}
