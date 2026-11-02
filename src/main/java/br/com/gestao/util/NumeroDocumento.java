package br.com.gestao.util;

import java.util.Locale;
import java.util.UUID;

public final class NumeroDocumento {
    private NumeroDocumento() {}

    public static String gerar(String prefixo) {
        return prefixo + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
