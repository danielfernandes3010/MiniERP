package br.com.gestao.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyUtilsTest {
    @Test
    void converteReaisParaCentavosEQuantidadeParaMilesimos() {
        assertEquals(12550, MoneyUtils.toCents(new BigDecimal("125.50")));
        assertEquals(new BigDecimal("125.50"), MoneyUtils.fromCents(12550));
        assertEquals(2500, MoneyUtils.toThousandths(new BigDecimal("2.500")));
    }

    @Test
    void rejeitaQuantidadeComMaisDeTresCasas() {
        assertThrows(ArithmeticException.class, () -> MoneyUtils.toThousandths(new BigDecimal("1.0001")));
    }
}
