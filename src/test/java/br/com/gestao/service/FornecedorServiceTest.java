package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.FornecedorRequest;
import br.com.gestao.entity.Fornecedor;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.repository.FornecedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {
    @Mock private FornecedorRepository repository;
    @InjectMocks private FornecedorService service;

    @Test
    @SuppressWarnings("null")
    void normalizaDocumentoEGravaFornecedorValido() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any(Fornecedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.criar(new FornecedorRequest("F-01", "Fornecedor válido", null,
                "529.982.247-25", "contato@example.com", null, null, null, "SP", "01001000"));

        assertEquals("52998224725", result.cpfCnpj());
        assertTrue(result.ativo());
        verify(repository).save(any(Fornecedor.class));
    }

    @Test
    @SuppressWarnings("null")
    void rejeitaDocumentoInvalidoSemPersistir() {
        var request = new FornecedorRequest("F-02", "Fornecedor inválido", null,
                "111.111.111-11", "contato@example.com", null, null, null, null, null);

        assertThrows(BusinessException.class, () -> service.criar(request));
        verify(repository, never()).save(any());
    }

    @Test
    @SuppressWarnings("null")
    void rejeitaCodigoDuplicado() {
        Fornecedor existing = new Fornecedor();
        existing.setId(5L);
        existing.setCodigo("F-03");
        existing.setCpfCnpj("04252011000110");
        when(repository.findAll()).thenReturn(List.of(existing));

        var request = new FornecedorRequest("F-03", "Fornecedor duplicado", null,
                "52998224725", "contato@example.com", null, null, null, null, null);

        assertThrows(BusinessException.class, () -> service.criar(request));
        verify(repository, never()).save(any());
    }
}
