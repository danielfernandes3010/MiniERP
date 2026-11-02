package br.com.gestao.integration;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.enums.FormaPagamento;
import br.com.gestao.enums.StatusPedido;
import br.com.gestao.enums.StatusTitulo;
import br.com.gestao.service.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ComprasFinanceiroIntegrationTest {
    private static final String DB_PATH = System.getProperty("java.io.tmpdir") + "\\mini-erp-it-" + UUID.randomUUID() + ".db";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DB_PATH);
    }

    @Autowired private FornecedorService fornecedorService;
    @Autowired private ProdutoService produtoService;
    @Autowired private PedidoCompraService pedidoService;
    @Autowired private EntradaEstoqueService entradaService;
    @Autowired private EstoqueService estoqueService;
    @Autowired private TituloPagarService tituloService;
    @Autowired private RelatorioService relatorioService;

    @Test
    void executaFluxoDeCompraRecebimentoEstoqueEPagamento() {
        FornecedorResponse supplier = fornecedorService.criar(new FornecedorRequest(
                "F-IT-01", "Fornecedor Integração", null, "529.982.247-25", "it@example.com",
                null, null, null, null, null));
        ProdutoResponse product = produtoService.criar(new ProdutoRequest("P-IT-01", "Produto Integração",
                "Geral", "UN", new BigDecimal("25.00"), new BigDecimal("2.000")));
        PedidoResponse order = pedidoService.criar(new PedidoRequest(supplier.id(), LocalDate.now().plusDays(4), 30,
                "Teste integrado", List.of(new ItemPedidoRequest(product.id(), new BigDecimal("10.000"), new BigDecimal("25.00")))));
        assertEquals(new BigDecimal("250.00"), order.valorTotal());
        order = pedidoService.confirmar(requiredId(order.id()));
        assertEquals(StatusPedido.EM_ABERTO, order.status());

        EntradaRequest firstRequest = new EntradaRequest(order.id(), LocalDate.now(), "entrada-it-1",
                List.of(new RecebimentoItemRequest(order.itens().getFirst().id(), new BigDecimal("4.000"))));
        EntradaResponse first = entradaService.registrar(firstRequest);
        EntradaResponse repeated = entradaService.registrar(firstRequest);
        assertEquals(first.id(), repeated.id());
        assertEquals(new BigDecimal("100.00"), first.valorTotal());
        assertEquals(new BigDecimal("4.000"), estoqueService.obter(requiredId(product.id())).saldoFisico());
        assertEquals(StatusPedido.PARCIALMENTE_RECEBIDO, pedidoService.obter(requiredId(order.id())).status());
        TituloResponse firstTitle = tituloService.obter(requiredId(first.tituloId()));
        assertEquals(new BigDecimal("100.00"), firstTitle.valorOriginal());
        assertEquals(LocalDate.now().plusDays(30), firstTitle.dataVencimento());

        EntradaResponse second = entradaService.registrar(new EntradaRequest(order.id(), LocalDate.now(), "entrada-it-2",
                List.of(new RecebimentoItemRequest(order.itens().getFirst().id(), new BigDecimal("6.000")))));
        assertEquals(new BigDecimal("150.00"), second.valorTotal());
        assertEquals(new BigDecimal("10.000"), estoqueService.obter(requiredId(product.id())).saldoFisico());
        assertEquals(StatusPedido.RECEBIDO, pedidoService.obter(requiredId(order.id())).status());

        BaixaRequest firstPayment = new BaixaRequest(LocalDate.now(),
                new BigDecimal("40.00"), FormaPagamento.PIX, null, "baixa-it-1");
        TituloResponse paidTitle = tituloService.baixar(requiredId(first.tituloId()), firstPayment);
        assertEquals(paidTitle.status(), tituloService.baixar(requiredId(first.tituloId()), firstPayment).status());
        assertEquals(1, tituloService.historico(requiredId(first.tituloId())).size());
        assertEquals(new BigDecimal("60.00"), paidTitle.saldoAberto());
        assertEquals(StatusTitulo.PARCIALMENTE_PAGO, paidTitle.status());
        paidTitle = tituloService.baixar(requiredId(first.tituloId()), new BaixaRequest(LocalDate.now(),
                new BigDecimal("60.00"), FormaPagamento.BOLETO, null, "baixa-it-2"));
        assertEquals(StatusTitulo.QUITADO, paidTitle.status());

        assertEquals(new BigDecimal("150.00"), tituloService.obter(requiredId(second.tituloId())).saldoAberto());
        assertEquals(2, relatorioService.financeiro(null, null, null, null, false, false)
                .get("titulos") instanceof List<?> list ? list.size() : 0);
    }

    @Test
    void rejeitaRecebimentosConcorrentesAcimaDoPedido() {
        FornecedorResponse supplier = fornecedorService.criar(new FornecedorRequest(
                "F-IT-02", "Fornecedor Concorrência", null, "04.252.011/0001-10", "conc@example.com",
                null, null, null, null, null));
        ProdutoResponse product = produtoService.criar(new ProdutoRequest("P-IT-02", "Produto Concorrência",
                null, "UN", new BigDecimal("3.00"), BigDecimal.ZERO));
        PedidoResponse order = pedidoService.criar(new PedidoRequest(supplier.id(), null, 0, null,
                List.of(new ItemPedidoRequest(product.id(), new BigDecimal("10.000"), new BigDecimal("3.00")))));
        order = pedidoService.confirmar(requiredId(order.id()));
        long itemId = order.itens().getFirst().id();
        EntradaRequest requestA = new EntradaRequest(order.id(), LocalDate.now(), "race-a",
                List.of(new RecebimentoItemRequest(itemId, new BigDecimal("6.000"))));
        EntradaRequest requestB = new EntradaRequest(order.id(), LocalDate.now(), "race-b",
                List.of(new RecebimentoItemRequest(itemId, new BigDecimal("6.000"))));
        CompletableFuture<Boolean> a = CompletableFuture.supplyAsync(() -> succeeds(requestA));
        CompletableFuture<Boolean> b = CompletableFuture.supplyAsync(() -> succeeds(requestB));
        int successCount = (a.join() ? 1 : 0) + (b.join() ? 1 : 0);
        assertEquals(1, successCount);
        assertEquals(new BigDecimal("6.000"), estoqueService.obter(requiredId(product.id())).saldoFisico());
        assertEquals(StatusPedido.PARCIALMENTE_RECEBIDO, pedidoService.obter(requiredId(order.id())).status());
    }

    @NonNull
    private Long requiredId(@Nullable Long id) {
        return Objects.requireNonNull(id, "A resposta persistida deve conter ID.");
    }

    private boolean succeeds(EntradaRequest request) {
        try {
            entradaService.registrar(request);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
