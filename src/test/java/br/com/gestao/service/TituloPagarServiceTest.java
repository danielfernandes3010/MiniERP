package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.BaixaRequest;
import br.com.gestao.entity.BaixaTitulo;
import br.com.gestao.entity.EntradaEstoque;
import br.com.gestao.entity.Fornecedor;
import br.com.gestao.entity.PedidoCompra;
import br.com.gestao.entity.TituloPagar;
import br.com.gestao.enums.FormaPagamento;
import br.com.gestao.enums.StatusTitulo;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.repository.BaixaTituloRepository;
import br.com.gestao.repository.TituloPagarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TituloPagarServiceTest {
    @Mock private TituloPagarRepository titulos;
    @Mock private BaixaTituloRepository baixas;
    @InjectMocks private TituloPagarService service;

    private @NonNull TituloPagar titulo = new TituloPagar();

    @BeforeEach
    void prepararTituloEmAberto() {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(2L);
        fornecedor.setRazaoSocial("Fornecedor Teste");

        PedidoCompra pedido = new PedidoCompra();
        pedido.setId(3L);
        pedido.setNumero("PC-TESTE");

        EntradaEstoque entrada = new EntradaEstoque();
        entrada.setId(4L);

        titulo = new TituloPagar();
        titulo.setId(1L);
        titulo.setNumero("TP-TESTE");
        titulo.setFornecedor(fornecedor);
        titulo.setPedido(pedido);
        titulo.setEntrada(entrada);
        titulo.setDataEmissao(LocalDate.now().minusDays(1));
        titulo.setDataVencimento(LocalDate.now().plusDays(29));
        titulo.setValorOriginalCentavos(10_000);
        titulo.setValorPagoCentavos(0);
        titulo.setSaldoAbertoCentavos(10_000);
        titulo.setStatus(StatusTitulo.EM_ABERTO);
    }

    @Test
    void registraPagamentoParcialEAtualizaSaldo() {
        prepararBaixaNova();
        when(titulos.save(anyNonNull(TituloPagar.class)))
                .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0, TituloPagar.class)));

        var response = emTransacao(() -> service.baixar(1L, request("40.00", "baixa-parcial")));

        assertEquals(new BigDecimal("40.00"), response.valorPago());
        assertEquals(new BigDecimal("60.00"), response.saldoAberto());
        assertEquals(StatusTitulo.PARCIALMENTE_PAGO, response.status());
        verify(baixas).save(anyNonNull(BaixaTitulo.class));
        verify(titulos).save(titulo);
    }

    @Test
    void registraPagamentoIntegralEQuítaTitulo() {
        prepararBaixaNova();
        when(titulos.save(anyNonNull(TituloPagar.class)))
                .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0, TituloPagar.class)));

        var response = emTransacao(() -> service.baixar(1L, request("100.00", "baixa-integral")));

        assertEquals(new BigDecimal("100.00"), response.valorPago());
        assertEquals(BigDecimal.ZERO.setScale(2), response.saldoAberto());
        assertEquals(StatusTitulo.QUITADO, response.status());
    }

    @Test
    void rejeitaPagamentoAcimaDoSaldoEmAberto() {
        prepararBaixaNova();

        assertThrows(BusinessException.class,
                () -> emTransacao(() -> service.baixar(1L, request("100.01", "baixa-excedente"))));

        verify(baixas, never()).save(anyNonNull(BaixaTitulo.class));
        verify(titulos, never()).save(anyNonNull(TituloPagar.class));
    }

    @Test
    void rejeitaPagamentoEmTituloQuitado() {
        titulo.setValorPagoCentavos(10_000);
        titulo.setSaldoAbertoCentavos(0);
        titulo.setStatus(StatusTitulo.QUITADO);
        prepararBaixaNova();

        assertThrows(BusinessException.class,
                () -> emTransacao(() -> service.baixar(1L, request("1.00", "baixa-apos-quitacao"))));

        verify(baixas, never()).save(anyNonNull(BaixaTitulo.class));
    }

    private void prepararBaixaNova() {
        when(baixas.findByChaveIdempotencia(any())).thenReturn(Optional.empty());
        when(titulos.findById(1L)).thenReturn(Optional.of(titulo));
    }

    private BaixaRequest request(String amount, String idempotencyKey) {
        return new BaixaRequest(LocalDate.now(), new BigDecimal(amount), FormaPagamento.PIX,
                null, idempotencyKey);
    }

    private <T> T emTransacao(Supplier<T> operation) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            return operation.get();
        } finally {
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @SuppressWarnings("null")
    @NonNull
    private static <T> T anyNonNull(Class<T> type) {
        return any(type);
    }
}
