package pl.pajak.dqr.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.pajak.dqr.engine.testing.TestRecord;

class ObservedPartnerRecordTest {

  @Nested
  class Delegation {

    @Test
    void answersTheDelegatesReference() {
      final var observed = new ObservedPartnerRecord(TestRecord.of("r-1"));

      assertThat(observed.refId()).isEqualTo("r-1");
    }

    @Test
    void answersTheDelegatesValue() {
      final var observed = observing(TestRecord.builder("r-1").withField("name", "ACME").build());

      assertThat(observed.getField("name")).isEqualTo("ACME");
    }

    @Test
    void answersNullForAFieldTheRecordDoesNotCarry() {
      final var observed = observing(TestRecord.of("r-1"));

      assertThat(observed.getField("name")).isNull();
    }

    @Test
    void doesNotSwallowARecordThatCannotBeRead() {
      final PartnerRecord unreadable =
          new PartnerRecord() {

            @Override
            public String refId() {
              return "r-1";
            }

            @Override
            public String getField(String name) {
              throw new IllegalStateException("record is unreadable");
            }
          };

      assertThatThrownBy(() -> observing(unreadable).getField("name"))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  class AccessedFields {

    @Test
    void areEmptyBeforeAnythingIsRead() {
      assertThat(observing(TestRecord.of("r-1")).getAccessedFields()).isEmpty();
    }

    @Test
    void keepTheFieldsThatWereRead() {
      final var observed =
          observing(
              TestRecord.builder("r-1")
                  .withField("name", "ACME")
                  .withField("city", "Wien")
                  .build());

      observed.getField("name");
      observed.getField("city");

      assertThat(observed.getAccessedFields())
          .containsExactly(entry("name", "ACME"), entry("city", "Wien"));
    }

    @Test
    void keepOnlyTheFieldsThatWereRead() {
      final var observed =
          observing(
              TestRecord.builder("r-1")
                  .withField("name", "ACME")
                  .withField("city", "Wien")
                  .build());

      observed.getField("name");

      assertThat(observed.getAccessedFields()).containsOnlyKeys("name");
    }

    /** The absence of a field is what explains a decision just as much as a value does. */
    @Test
    void keepAnAbsentFieldWithANullValue() {
      final var observed = observing(TestRecord.of("r-1"));

      observed.getField("vatId");

      assertThat(observed.getAccessedFields()).containsExactly(entry("vatId", null));
    }

    @Test
    void keepTheOrderTheyWereReadIn() {
      final var observed =
          observing(
              TestRecord.builder("r-1")
                  .withField("a", "1")
                  .withField("b", "2")
                  .withField("c", "3")
                  .build());

      observed.getField("c");
      observed.getField("a");
      observed.getField("b");

      assertThat(observed.getAccessedFields())
          .containsExactly(entry("c", "3"), entry("a", "1"), entry("b", "2"));
    }

    @Test
    void recordAFieldReadTwiceOnlyOnce() {
      final var reads = new AtomicInteger();
      final PartnerRecord counting =
          new PartnerRecord() {

            @Override
            public String refId() {
              return "r-1";
            }

            @Override
            public String getField(String name) {
              return "read-" + reads.incrementAndGet();
            }
          };
      final var observed = observing(counting);

      observed.getField("name");
      observed.getField("name");

      assertThat(observed.getAccessedFields()).containsExactly(entry("name", "read-2"));
    }
  }

  @Nested
  class TheReturnedMap {

    @Test
    void cannotBeChangedByTheCaller() {
      final var observed = observing(TestRecord.builder("r-1").withField("name", "ACME").build());
      observed.getField("name");

      final Map<String, String> accessed = observed.getAccessedFields();

      assertThatThrownBy(() -> accessed.put("city", "Wien"))
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isASnapshotThatLaterReadsDoNotReachInto() {
      final var observed =
          observing(
              TestRecord.builder("r-1")
                  .withField("name", "ACME")
                  .withField("city", "Wien")
                  .build());
      observed.getField("name");

      final Map<String, String> accessed = observed.getAccessedFields();
      observed.getField("city");

      assertThat(accessed).containsOnlyKeys("name");
      assertThat(observed.getAccessedFields()).containsOnlyKeys("name", "city");
    }
  }

  private static ObservedPartnerRecord observing(PartnerRecord delegate) {
    return new ObservedPartnerRecord(delegate);
  }
}
