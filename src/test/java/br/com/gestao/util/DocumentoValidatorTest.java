package br.com.gestao.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoValidatorTest {
    @Test
    void validaCpfCnpjERejeitaSequenciasInvalidas() {
        assertTrue(DocumentoValidator.valid("529.982.247-25"));
        assertTrue(DocumentoValidator.valid("04.252.011/0001-10"));
        assertFalse(DocumentoValidator.valid("111.111.111-11"));
        assertFalse(DocumentoValidator.valid("04.252.011/0001-11"));
    }

    @Test
    void normalizaDocumento() {
        assertEquals("04252011000110", DocumentoValidator.normalize("04.252.011/0001-10"));
    }
}
