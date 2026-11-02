package br.com.gestao.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
    private MoneyUtils() {}

    public static long toCents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    public static BigDecimal fromCents(long value) {
        return BigDecimal.valueOf(value, 2);
    }

    public static long toThousandths(BigDecimal value) {
        return value.setScale(3, RoundingMode.UNNECESSARY).movePointRight(3).longValueExact();
    }

    public static BigDecimal fromThousandths(long value) {
        return BigDecimal.valueOf(value, 3);
    }
}
