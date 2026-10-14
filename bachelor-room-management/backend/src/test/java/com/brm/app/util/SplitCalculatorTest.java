package com.brm.app.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitCalculatorTest {

    @Test
    void leftoverCentsGoToTheFirstPeople() {
        Map<String, BigDecimal> split = SplitCalculator.equalSplit(new BigDecimal("100.01"), List.of("A", "B"));

        assertThat(split.get("A")).isEqualByComparingTo("50.01");
        assertThat(split.get("B")).isEqualByComparingTo("50.00");
    }

    @Test
    void equalSplitAlwaysAddsUpToTheTotal() {
        Map<String, BigDecimal> split = SplitCalculator.equalSplit(new BigDecimal("100"), List.of("A", "B", "C"));

        assertThat(split.get("A")).isEqualByComparingTo("33.34");
        assertThat(split.get("B")).isEqualByComparingTo("33.33");
        assertThat(split.get("C")).isEqualByComparingTo("33.33");
        assertThat(split.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
    }

    @Test
    void rejectsInvalidEqualSplits() {
        assertThatThrownBy(() -> SplitCalculator.equalSplit(BigDecimal.ZERO, List.of("A")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SplitCalculator.equalSplit(new BigDecimal("10"), List.<String>of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SplitCalculator.equalSplit(new BigDecimal("10.005"), List.of("A")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void customSplitMustAddUpToTheTotal() {
        Map<String, BigDecimal> good = new LinkedHashMap<>();
        good.put("A", new BigDecimal("60"));
        good.put("B", new BigDecimal("40"));
        assertThat(SplitCalculator.customSplit(new BigDecimal("100"), good)).hasSize(2);

        Map<String, BigDecimal> bad = new LinkedHashMap<>();
        bad.put("A", new BigDecimal("60"));
        bad.put("B", new BigDecimal("30"));
        assertThatThrownBy(() -> SplitCalculator.customSplit(new BigDecimal("100"), bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("add up");
    }

    @Test
    void customSplitDropsZeroSharesAndRejectsNegativeOnes() {
        Map<String, BigDecimal> withZero = new LinkedHashMap<>();
        withZero.put("A", new BigDecimal("100"));
        withZero.put("B", BigDecimal.ZERO);
        assertThat(SplitCalculator.customSplit(new BigDecimal("100"), withZero)).containsOnlyKeys("A");

        Map<String, BigDecimal> negative = new LinkedHashMap<>();
        negative.put("A", new BigDecimal("120"));
        negative.put("B", new BigDecimal("-20"));
        assertThatThrownBy(() -> SplitCalculator.customSplit(new BigDecimal("100"), negative))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
