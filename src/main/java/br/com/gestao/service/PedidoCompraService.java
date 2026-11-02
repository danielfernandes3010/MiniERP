package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.entity.*;
import br.com.gestao.enums.StatusPedido;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.exception.ResourceNotFoundException;
import br.com.gestao.repository.*;
import br.com.gestao.mapper.PedidoCompraMapper;
import br.com.gestao.util.NumeroDocumento;
import br.com.gestao.util.MoneyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PedidoCompraService {
    private final PedidoCompraRepository pedidos;
    private final FornecedorRepository fornecedores;
    private final ProdutoRepository produtos;

    public PedidoCompraService(PedidoCompraRepository pedidos, FornecedorRepository fornecedores, ProdutoRepository produtos) {
        this.pedidos = pedidos;
        this.fornecedores = fornecedores;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() { return pedidos.findAllByOrderByCriadoEmDesc().stream().map(this::response).toList(); }
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long fornecedorId, StatusPedido status) {
        return pedidos.findAllByOrderByCriadoEmDesc().stream()
                .filter(p -> fornecedorId == null || p.getFornecedor().getId().equals(fornecedorId))
                .filter(p -> status == null || p.getStatus() == status)
                .map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public List<PedidoResponse> porFornecedor(@NonNull Long fornecedorId) {
        if (!fornecedores.existsById(fornecedorId)) throw new ResourceNotFoundException("Fornecedor");
        return pedidos.findByFornecedorIdOrderByCriadoEmDesc(fornecedorId).stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public PedidoResponse obter(@NonNull Long id) { return response(find(id)); }

    @Transactional
    public PedidoResponse criar(PedidoRequest request) {
        PedidoCompra pedido = new PedidoCompra();
        pedido.setNumero(NumeroDocumento.gerar("PC-"));
        pedido.setDataEmissao(LocalDate.now());
        pedido.setStatus(StatusPedido.RASCUNHO);
        preencher(pedido, request);
        return response(pedidos.save(pedido));
    }

    @Transactional
    public PedidoResponse atualizar(@NonNull Long id, PedidoRequest request) {
        PedidoCompra pedido = find(id);
        if (pedido.getStatus() != StatusPedido.RASCUNHO)
            throw new BusinessException("Somente pedidos em rascunho podem ser editados.");
        preencher(pedido, request);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return response(pedidos.save(pedido));
    }

    @Transactional
    public PedidoResponse confirmar(@NonNull Long id) {
        PedidoCompra pedido = find(id);
        if (pedido.getStatus() != StatusPedido.RASCUNHO)
            throw new BusinessException("Apenas pedidos em rascunho podem ser confirmados.");
        if (pedido.getItens().isEmpty() || pedido.getValorTotalCentavos() <= 0)
            throw new BusinessException("Pedido sem itens ou com valor inválido.", HttpStatus.BAD_REQUEST);
        if (!pedido.getFornecedor().isAtivo()) throw new BusinessException("Fornecedor inativo não pode receber pedidos.");
        if (pedido.getItens().stream().anyMatch(i -> !i.getProduto().isAtivo()))
            throw new BusinessException("Pedido contém produto inativo.");
        pedido.setStatus(StatusPedido.EM_ABERTO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return response(pedidos.save(pedido));
    }

    @Transactional
    public PedidoResponse cancelar(@NonNull Long id) {
        PedidoCompra pedido = find(id);
        if (pedido.getStatus() != StatusPedido.RASCUNHO && pedido.getStatus() != StatusPedido.EM_ABERTO)
            throw new BusinessException("Somente pedidos em rascunho ou em aberto podem ser cancelados.");
        if (pedido.getItens().stream().anyMatch(i -> i.getQuantidadeRecebidaMilesimos() > 0))
            throw new BusinessException("Pedido com recebimento não pode ser cancelado.");
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        return response(pedidos.save(pedido));
    }

    private void preencher(PedidoCompra pedido, PedidoRequest request) {
        Fornecedor fornecedor = fornecedores.findById(Objects.requireNonNull(request.fornecedorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor"));
        if (!fornecedor.isAtivo()) throw new BusinessException("Fornecedor inativo não pode receber pedidos.");
        if (request.previsaoEntrega() != null && request.previsaoEntrega().isBefore(LocalDate.now()))
            throw new BusinessException("Previsão de entrega não pode estar no passado.", HttpStatus.BAD_REQUEST);
        Set<Long> unique = new HashSet<>();
        List<ItemPedido> novos = new ArrayList<>();
        long total = 0;
        for (ItemPedidoRequest item : request.itens()) {
            if (!unique.add(item.produtoId())) throw new BusinessException("Não repita produtos no mesmo pedido.", HttpStatus.BAD_REQUEST);
            Produto produto = produtos.findById(Objects.requireNonNull(item.produtoId())).orElseThrow(() -> new ResourceNotFoundException("Produto"));
            if (!produto.isAtivo()) throw new BusinessException("Produto inativo não pode ser incluído no pedido.");
            long quantity = MoneyUtils.toThousandths(item.quantidade());
            long unit = MoneyUtils.toCents(item.valorUnitario());
            if (quantity <= 0 || unit <= 0) throw new BusinessException("Quantidade e preço unitário devem ser positivos.", HttpStatus.BAD_REQUEST);
            long subtotal = BigDecimal.valueOf(unit).multiply(BigDecimal.valueOf(quantity))
                    .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).longValueExact();
            ItemPedido line = new ItemPedido();
            line.setPedido(pedido);
            line.setProduto(produto);
            line.setQuantidadeSolicitadaMilesimos(quantity);
            line.setQuantidadeRecebidaMilesimos(0);
            line.setValorUnitarioCentavos(unit);
            line.setValorTotalCentavos(subtotal);
            novos.add(line);
            total = Math.addExact(total, subtotal);
        }
        pedido.setFornecedor(fornecedor);
        pedido.setPrevisaoEntrega(request.previsaoEntrega());
        pedido.setPrazoPagamentoDias(request.prazoPagamentoDias());
        pedido.setObservacoes(request.observacoes());
        pedido.getItens().clear();
        pedido.getItens().addAll(novos);
        pedido.setValorTotalCentavos(total);
        pedido.setAtualizadoEm(LocalDateTime.now());
    }

    private PedidoCompra find(@NonNull Long id) { return pedidos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido")); }
    private PedidoResponse response(PedidoCompra p) {
        return PedidoCompraMapper.toResponse(p);
    }
}
