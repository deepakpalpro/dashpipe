package com.dashflow.api.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** W4-US02: completeness formula §7.4. */
class CompletenessCalculatorTest {

  @Test
  void fullCompleteness_isOne() {
    CompletenessCalculator.Result r = CompletenessCalculator.calculate(100, 100);
    assertThat(r.ratio()).isCloseTo(1.0, within(1e-9));
    assertThat(r.percent()).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  @Test
  void fixture_ninetyEightOfHundred() {
    CompletenessCalculator.Result r = CompletenessCalculator.calculate(100, 98);
    assertThat(r.ratio()).isCloseTo(0.98, within(1e-9));
    assertThat(r.percent()).isEqualByComparingTo(new BigDecimal("98.00"));
  }

  @Test
  void zeroRecordsIn_isZero() {
    CompletenessCalculator.Result r = CompletenessCalculator.calculate(0, 5);
    assertThat(r.ratio()).isEqualTo(0.0);
    assertThat(r.percent()).isEqualByComparingTo(new BigDecimal("0.00"));
  }

  @Test
  void stubStage_oneInOneOut() {
    CompletenessCalculator.Result r = CompletenessCalculator.calculate(1, 1);
    assertThat(r.ratio()).isEqualTo(1.0);
    assertThat(r.percent()).isEqualByComparingTo(new BigDecimal("100.00"));
  }
}
