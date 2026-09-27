package pl.pajak.dqr.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.engine.rule.RuleCatalog;
import pl.pajak.dqr.engine.rule.RuleStatus;
import pl.pajak.dqr.engine.testing.FakeRecordSource;
import pl.pajak.dqr.engine.testing.FakeRuleCatalog;
import pl.pajak.dqr.engine.testing.RecordingSink;

class EngineInputTest {

  private final RuleCatalog catalog = FakeRuleCatalog.of();
  private final PartnerRecordSource source = FakeRecordSource.empty();
  private final RuleResultSink sink = new RecordingSink();

  @Nested
  class Defaults {

    @Test
    void readsTheSourceInBatchesOfAHundred() {
      assertThat(built(EngineInput.builder(catalog, source, sink)).batchSize()).isEqualTo(100);
    }

    @Test
    void evaluatesReleasedRulesOnly() {
      assertThat(built(EngineInput.builder(catalog, source, sink)).ruleFilter())
          .isEqualTo(RuleFilter.status(RuleStatus.RELEASED));
    }
  }

  @Nested
  class RequiredSettings {

    @Test
    void areCarriedThroughToTheInput() {
      final var input = built(EngineInput.builder(catalog, source, sink));

      assertThat(input.ruleCatalog()).isSameAs(catalog);
      assertThat(input.recordSource()).isSameAs(source);
      assertThat(input.resultSink()).isSameAs(sink);
    }

    @Test
    void aMissingCatalogIsRejected() {
      assertThatNullPointerException()
          .isThrownBy(() -> EngineInput.builder(null, source, sink).build());
    }

    @Test
    void aMissingSourceIsRejected() {
      assertThatNullPointerException()
          .isThrownBy(() -> EngineInput.builder(catalog, null, sink).build());
    }

    @Test
    void aMissingSinkIsRejected() {
      assertThatNullPointerException()
          .isThrownBy(() -> EngineInput.builder(catalog, source, null).build());
    }
  }

  @Nested
  class OptionalSettings {

    @Test
    void anExplicitBatchSizeReplacesTheDefault() {
      final var input = built(EngineInput.builder(catalog, source, sink).withBatchSize(500));

      assertThat(input.batchSize()).isEqualTo(500);
    }

    @Test
    void anExplicitFilterReplacesTheDefault() {
      final var filter = RuleFilter.category("address");

      final var input = built(EngineInput.builder(catalog, source, sink).withRuleFilter(filter));

      assertThat(input.ruleFilter()).isSameAs(filter);
    }

    @Test
    void aBatchSizeOfOneIsStillARun() {
      assertThatCode(() -> EngineInput.builder(catalog, source, sink).withBatchSize(1).build())
          .doesNotThrowAnyException();
    }

    @Test
    void aBatchSizeThatWouldNeverReadARecordIsRejected() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> EngineInput.builder(catalog, source, sink).withBatchSize(0).build());
      assertThatIllegalArgumentException()
          .isThrownBy(() -> EngineInput.builder(catalog, source, sink).withBatchSize(-1).build());
    }

    @Test
    void aMissingFilterIsRejected() {
      assertThatNullPointerException()
          .isThrownBy(() -> EngineInput.builder(catalog, source, sink).withRuleFilter(null));
    }
  }

  @Nested
  class ValidationTiming {

    /**
     * A filter is checked where it is handed over, because a null there is a mistake in the calling
     * code rather than a value worth carrying to {@code build()}.
     */
    @Test
    void aNullFilterFailsAtTheSetterRatherThanAtBuild() {
      final var builder = EngineInput.builder(catalog, source, sink);

      assertThatNullPointerException().isThrownBy(() -> builder.withRuleFilter(null));
      assertThatCode(builder::build).doesNotThrowAnyException();
    }

    /** A batch size is a value the builder carries; only the built input has to make sense. */
    @Test
    void aNonPositiveBatchSizeFailsAtBuildRatherThanAtTheSetter() {
      final var builder = EngineInput.builder(catalog, source, sink);

      assertThatCode(() -> builder.withBatchSize(0)).doesNotThrowAnyException();
      assertThatIllegalArgumentException().isThrownBy(builder::build);
    }

    @Test
    void aRejectedBatchSizeCanBeCorrectedBeforeBuilding() {
      final var builder = EngineInput.builder(catalog, source, sink).withBatchSize(0);

      assertThat(built(builder.withBatchSize(25)).batchSize()).isEqualTo(25);
    }
  }

  @Nested
  class Immutability {

    @Test
    void aBuiltInputDoesNotFollowLaterBuilderChanges() {
      final var builder = EngineInput.builder(catalog, source, sink).withBatchSize(10);
      final var input = built(builder);

      builder.withBatchSize(20).withRuleFilter(RuleFilter.all());

      assertThat(input.batchSize()).isEqualTo(10);
      assertThat(input.ruleFilter()).isEqualTo(RuleFilter.status(RuleStatus.RELEASED));
    }

    @Test
    void theSameBuilderCanProduceSeveralInputs() {
      final var builder = EngineInput.builder(catalog, source, sink);

      final var first = built(builder.withBatchSize(10));
      final var second = built(builder.withBatchSize(20));

      assertThat(first.batchSize()).isEqualTo(10);
      assertThat(second.batchSize()).isEqualTo(20);
    }
  }

  private static EngineInputImpl built(EngineInput.Builder builder) {
    return switch (builder.build()) {
      case EngineInputImpl impl -> impl;
    };
  }
}
