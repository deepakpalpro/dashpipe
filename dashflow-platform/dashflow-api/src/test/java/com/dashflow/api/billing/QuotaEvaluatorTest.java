package com.dashflow.api.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** W5-US04: soft / hard / zero-credit matrix. */
class QuotaEvaluatorTest {

  private QuotaEvaluator evaluator;
  private QuotaConfigParser parser;

  @BeforeEach
  void setUp() {
    evaluator = new QuotaEvaluator();
    parser = new QuotaConfigParser(new ObjectMapper());
  }

  @Test
  void allow_whenUnderSoftAndHasCredit() {
    QuotaConfig config =
        parser.parse(
            """
            {"dimensions":{"platform.pipeline_runs":{"soft":100,"hard":200}}}
            """);
    QuotaDecision d =
        evaluator.evaluate(
            new BigDecimal("10.0000"),
            config,
            Map.of("platform.pipeline_runs", new BigDecimal("50")));

    assertThat(d.code()).isEqualTo(QuotaDecisionCode.ALLOW);
    assertThat(d.allowed()).isTrue();
    assertThat(d.blocksRun()).isFalse();
  }

  @Test
  void softWarn_whenAtOrAboveSoftBelowHard() {
    QuotaConfig config =
        parser.parse(
            """
            {"dimensions":{"platform.pipeline_runs":{"soft":100,"hard":200}}}
            """);
    QuotaDecision d =
        evaluator.evaluate(
            new BigDecimal("5.0000"),
            config,
            Map.of("platform.pipeline_runs", new BigDecimal("100")));

    assertThat(d.code()).isEqualTo(QuotaDecisionCode.SOFT_WARN);
    assertThat(d.allowed()).isTrue();
    assertThat(d.blocksRun()).isFalse();
    assertThat(d.breachedDimension()).isEqualTo("platform.pipeline_runs");
    assertThat(d.softLimit()).isEqualByComparingTo("100");
  }

  @Test
  void hardBlock_whenAtOrAboveHard() {
    QuotaConfig config =
        parser.parse(
            """
            {"dimensions":{"data.records_processed":{"soft":1000,"hard":2000}}}
            """);
    QuotaDecision d =
        evaluator.evaluate(
            new BigDecimal("50.0000"),
            config,
            Map.of("data.records_processed", new BigDecimal("2000")));

    assertThat(d.code()).isEqualTo(QuotaDecisionCode.HARD_BLOCK);
    assertThat(d.allowed()).isFalse();
    assertThat(d.blocksRun()).isTrue();
    assertThat(d.breachedDimension()).isEqualTo("data.records_processed");
    assertThat(d.hardLimit()).isEqualByComparingTo("2000");
  }

  @Test
  void noCredit_whenBalanceZero_evenIfUnderQuota() {
    QuotaConfig config =
        parser.parse(
            """
            {"dimensions":{"platform.pipeline_runs":{"soft":100,"hard":200}}}
            """);
    QuotaDecision d =
        evaluator.evaluate(
            BigDecimal.ZERO, config, Map.of("platform.pipeline_runs", BigDecimal.ONE));

    assertThat(d.code()).isEqualTo(QuotaDecisionCode.NO_CREDIT);
    assertThat(d.blocksRun()).isTrue();
  }

  @Test
  void noCredit_whenBalanceNegative() {
    QuotaDecision d =
        evaluator.evaluate(new BigDecimal("-0.0100"), QuotaConfig.empty(), Map.of());

    assertThat(d.code()).isEqualTo(QuotaDecisionCode.NO_CREDIT);
  }

  @Test
  void hardTakesPrecedenceOverSoft_whenBothBreached() {
    QuotaConfig config =
        parser.parse(
            """
            {"dimensions":{
              "platform.pipeline_runs":{"soft":10,"hard":20},
              "data.records_processed":{"soft":100,"hard":200}
            }}
            """);
    QuotaDecision d =
        evaluator.evaluate(
            new BigDecimal("1.0000"),
            config,
            Map.of(
                "platform.pipeline_runs", new BigDecimal("15"),
                "data.records_processed", new BigDecimal("250")));

    assertThat(d.code()).isEqualTo(QuotaDecisionCode.HARD_BLOCK);
    assertThat(d.breachedDimension()).isEqualTo("data.records_processed");
  }

  @Test
  void emptyConfig_allowsWhenCreditPositive() {
    QuotaDecision d =
        evaluator.evaluate(new BigDecimal("1.0000"), QuotaConfig.empty(), Map.of());
    assertThat(d.code()).isEqualTo(QuotaDecisionCode.ALLOW);
  }

  @Test
  void parse_invalidJson_yieldsEmpty() {
    assertThat(parser.parse("not-json").dimensions()).isEmpty();
    assertThat(parser.parse(null).dimensions()).isEmpty();
  }
}
