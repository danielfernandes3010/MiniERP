package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.ItemPedidoRequest;
import br.com.gestao.dto.ApiDtos.PedidoRequest;
import br.com.gestao.entity.Fornecedor;
import br.com.gestao.entity.PedidoCompra;
import br.com.gestao.entity.Produto;
import br.com.gestao.enums.StatusPedido;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.repository.FornecedorRepository;
import br.com.gestao.repository.PedidoCompraRepository;
import br.com.gestao.repository.ProdutoRepository;
import org.springframework.lang.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoCompraServiceTest {
    @Mock private PedidoCompraRepository pedidos;
    @Mock private FornecedorRepository fornecedores;
    @Mock private ProdutoRepository produtos;
    @InjectMocks private PedidoCompraService service;

    private Fornecedor fornecedor;
    private Produto produto1;
    private Produto produto2;

    @BeforeEach
    void criarCadastrosAtivos() {
        fornecedor = new Fornecedor();
        fornecedor.setId(1L);
        fornecedor.setAtivo(true);

        produto1 = produto(10L, "A");
        produto2 = produto(20L, "B");
    }

    @Test
    void calculaSubtotalArredondadoEValorTotalComMultiplosItens() {
        prepararConsultasDeCadastro();
        when(produtos.findById(20L)).thenReturn(Optional.of(produto2));
        when(pedidos.save(anyNonNull(PedidoCompra.class)))
                .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0, PedidoCompra.class)));
        PedidoRequest request = new PedidoRequest(1L, null, 30, null, List.of(
                new ItemPedidoRequest(10L, new BigDecimal("1.500"), new BigDecimal("2.35")),
                new ItemPedidoRequest(20L, new BigDecimal("2.000"), new BigDecimal("10.00"))));

        var response = service.criar(request);

        assertEquals(new BigDecimal("23.53"), response.valorTotal());
        assertEquals(new BigDecimal("3.53"), response.itens().get(0).valorTotal());
        assertEquals(new BigDecimal("20.00"), response.itens().get(1).valorTotal());
        assertEquals(StatusPedido.RASCUNHO, response.status());
    }

    @Test
    void rejeitaConfirmacaoDePedidoSemItens() {
        PedidoCompra order = new PedidoCompra();
        order.setId(7L);
        order.setFornecedor(fornecedor);
        order.setStatus(StatusPedido.RASCUNHO);
        order.setItens(new java.util.ArrayList<>());
        when(pedidos.findById(7L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> service.confirmar(7L));
    }

    @Test
    void rejeitaQuantidadeZeroEPrecoUnitarioZero() {
        prepararConsultasDeCadastro();
        assertThrows(BusinessException.class, () -> service.criar(new PedidoRequest(1L, null, 30, null,
                List.of(new ItemPedidoRequest(10L, BigDecimal.ZERO, new BigDecimal("1.00"))))));
        assertThrows(BusinessException.class, () -> service.criar(new PedidoRequest(1L, null, 30, null,
                List.of(new ItemPedidoRequest(10L, BigDecimal.ONE, BigDecimal.ZERO)))));

        verify(pedidos, never()).save(anyNonNull(PedidoCompra.class));
    }

    @Test
    void impedeConfirmacaoDeFornecedorInativo() {
        Fornecedor inactiveSupplier = new Fornecedor();
        inactiveSupplier.setId(1L);
        inactiveSupplier.setAtivo(false);
        PedidoCompra order = pedidoRascunho(inactiveSupplier);
        when(pedidos.findById(7L)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> service.confirmar(7L));
    }

    @Test
    void confirmaRascunhoEImpossibilitaEdicaoDepoisDaConfirmacao() {
        PedidoCompra order = pedidoRascunho(fornecedor);
        when(pedidos.findById(7L)).thenReturn(Optional.of(order));
        when(pedidos.save(anyNonNull(PedidoCompra.class)))
                .thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0, PedidoCompra.class)));

        var confirmed = service.confirmar(7L);

        assertEquals(StatusPedido.EM_ABERTO, confirmed.status());
        assertThrows(BusinessException.class, () -> service.atualizar(7L,
                new PedidoRequest(1L, null, 30, null,
                        List.of(new ItemPedidoRequest(10L, BigDecimal.ONE, BigDecimal.ONE)))));
    }

    private void prepararConsultasDeCadastro() {
        when(fornecedores.findById(1L)).thenReturn(Optional.of(fornecedor));
        when(produtos.findById(10L)).thenReturn(Optional.of(produto1));
    }

    private PedidoCompra pedidoRascunho(Fornecedor orderSupplier) {
        PedidoCompra order = new PedidoCompra();
        order.setId(7L);
        order.setNumero("PC-TESTE");
        order.setFornecedor(orderSupplier);
        order.setStatus(StatusPedido.RASCUNHO);
        order.setItens(new java.util.ArrayList<>());
        var item = new br.com.gestao.entity.ItemPedido();
        item.setId(70L);
        item.setPedido(order);
        item.setProduto(produto1);
        item.setQuantidadeSolicitadaMilesimos(1000);
        item.setValorUnitarioCentavos(100);
        item.setValorTotalCentavos(100);
        order.getItens().add(item);
        order.setValorTotalCentavos(100);
        return order;
    }

    private Produto produto(long id, String code) {
        Produto product = new Produto();
        product.setId(id);
        product.setCodigo(code);
        product.setDescricao("Produto " + code);
        product.setUnidadeMedida("UN");
        product.setAtivo(true);
        return product;
    }

    @SuppressWarnings("null")
    @NonNull
    private static <T> T anyNonNull(Class<T> type) {
        return any(type);
    }
}
