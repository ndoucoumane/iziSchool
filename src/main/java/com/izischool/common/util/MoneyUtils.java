package com.izischool.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    public static final int DEFAULT_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtils() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal of(double value) {
        return BigDecimal.valueOf(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal of(long value) {
        return BigDecimal.valueOf(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal of(String value) {
        return new BigDecimal(value).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal scale(BigDecimal amount) {
        return amount == null ? zero() : amount.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isZero(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isGreaterThanOrEqual(BigDecimal first, BigDecimal second) {
        if (first == null) first = BigDecimal.ZERO;
        if (second == null) second = BigDecimal.ZERO;
        return first.compareTo(second) >= 0;
    }

    public static boolean isLessThan(BigDecimal first, BigDecimal second) {
        if (first == null) first = BigDecimal.ZERO;
        if (second == null) second = BigDecimal.ZERO;
        return first.compareTo(second) < 0;
    }

    public static BigDecimal subtract(BigDecimal base, BigDecimal sub) {
        BigDecimal b = base != null ? base : BigDecimal.ZERO;
        BigDecimal s = sub != null ? sub : BigDecimal.ZERO;
        return b.subtract(s).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal add(BigDecimal first, BigDecimal second) {
        BigDecimal f = first != null ? first : BigDecimal.ZERO;
        BigDecimal s = second != null ? second : BigDecimal.ZERO;
        return f.add(s).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }
}
