package com.brm.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure money-splitting logic (no Spring/JPA) so it can be unit tested in isolation.
 * All amounts are exact two-decimal values and a split always adds up to the total.
 */
public final class SplitCalculator {

    private SplitCalculator() {
    }

    /** Normalises to exactly 2 decimals, rejecting values like 10.005 instead of silently rounding. */
    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amounts can have at most 2 decimal places");
        }
    }

    /**
     * Splits {@code total} evenly. Leftover cents go one each to the first participants, in the
     * order given, so 100.01 between two people is 50.01 + 50.00.
     */
    public static <K> Map<K, BigDecimal> equalSplit(BigDecimal total, List<K> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Pick at least one person to split between");
        }
        BigDecimal exact = money(total);
        if (exact.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        long cents = exact.movePointRight(2).longValueExact();
        int people = participants.size();
        long base = cents / people;
        long remainder = cents % people;

        Map<K, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < people; i++) {
            long share = base + (i < remainder ? 1 : 0);
            result.put(participants.get(i), BigDecimal.valueOf(share, 2));
        }
        return result;
    }

    /** Validates admin-entered shares: none negative, at least one positive, and they add up to the total. */
    public static <K> Map<K, BigDecimal> customSplit(BigDecimal total, Map<K, BigDecimal> shares) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Enter a share for at least one person");
        }
        BigDecimal exactTotal = money(total);
        if (exactTotal.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Map<K, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        for (Map.Entry<K, BigDecimal> entry : shares.entrySet()) {
            BigDecimal amount = money(entry.getValue());
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("Shares can't be negative");
            }
            if (amount.signum() == 0) {
                continue; // a zero share means "not part of this expense"
            }
            result.put(entry.getKey(), amount);
            sum = sum.add(amount);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("At least one share must be more than zero");
        }
        if (sum.compareTo(exactTotal) != 0) {
            throw new IllegalArgumentException("Shares add up to " + sum.toPlainString()
                    + " but the expense total is " + exactTotal.toPlainString());
        }
        return result;
    }
}
