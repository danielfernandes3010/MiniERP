package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.entity.*;
import br.com.gestao.enums.StatusPedido;
import br.com.gestao.enums.StatusTitulo;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.exception.ResourceNotFoundException;
import br.com.gestao.repository.*;
import br.com.gestao.util.MoneyUtils;
import br.com.gestao.util.SqliteWriteLock;
import br.com.gestao.util.NumeroDocumento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class EntradaEstoqueService {
    private static final Logger log = LoggerFactory.getLogger(EntradaEstoqueService.class);
    private final EntradaEstoqueRepository entradas;
    private final PedidoCompraRepository pedidos;
    private final SaldoEstoqueRepository saldos;
    private final MovimentacaoEstoqueRepository movimentacoes;
    private final TituloPagarRepository titulos;

    public EntradaEstoqueService(EntradaEstoqueRepository entradas, PedidoCompraRepository pedidos,
            SaldoEstoqueRepository saldos, MovimentacaoEstoqueRepository movimentacoes,
            TituloPagarRepository titulos) {
        this.entradas = entradas;
        this.pedidos = pedidos;
        this.saldos = saldos;
        this.movimentacoes = movimentacoes;
        this.titulos = titulos;
    }

    @Transactional(readOnly = true)
    public List<EntradaResponse> listar() { return entradas.findAllByOrderByCriadoEmDesc().stream().map(this::response).toList(); }
    @Transactional(readOnly = true)
    public List<EntradaResponse> porPedido(@NonNull Long pedidoId) { return entradas.findByPedidoIdOrderByCriadoEmDesc(pedidoId).stream().map(this::response).toList(); }
    @Transactional(readOnly = true)
    public EntradaResponse obter(@NonNull Long id) { return response(entradas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entrada"))); }

    @Transactional
    public EntradaResponse registrar(EntradaRequest request) {
        SqliteWriteLock.lockUntilTransactionCompletes();
        var existing = entradas.findByChaveIdempotencia(request.chaveIdempotencia());
        if (existing.isPresent()) return response(existing.get());
        if (request.dataEntrada().isAfter(LocalDate.now()))
            throw new BusinessException("Data de entrada não pode ser futura.", HttpStatus.BAD_REQUEST);
        PedidoCompra pedido = pedidos.findById(Objects.requireNonNull(request.pedidoId())).orElseThrow(() -> new ResourceNotFoundException("Pedido"));
        if (pedido.getStatus() != StatusPedido.EM_ABERTO && pedido.getStatus() != StatusPedido.PARCIALMENTE_RECEBIDO)
            throw new BusinessException("O pedido precisa estar confirmado e não pode estar cancelado ou recebido.");
        Set<Long> itemIds = new HashSet<>();
        long total = 0;
        EntradaEstoque entrada = new EntradaEstoque();
        entrada.setNumero(NumeroDocumento.gerar("EN-"));
        entrada.setPedido(pedido);
        entrada.setDataEntrada(request.dataEntrada());
        entrada.setChaveIdempotencia(request.chaveIdempotencia().trim());
        entrada.setCriadoEm(LocalDateTime.now());
        for (RecebimentoItemRequest line : request.itens()) {
            if (!itemIds.add(line.itemPedidoId()))
                throw new BusinessException("O item não pode aparecer mais de uma vez na entrada.", HttpStatus.BAD_REQUEST);
            ItemPedido pedidoItem = pedido.getItens().stream().filter(i -> i.getId().equals(line.itemPedidoId())).findFirst()
                    .orElseThrow(() -> new BusinessException("Item não pertence ao pedido informado.", HttpStatus.BAD_REQUEST));
            long quantity = MoneyUtils.toThousandths(line.quantidadeRecebida());
            long pending = pedidoItem.getQuantidadeSolicitadaMilesimos() - pedidoItem.getQuantidadeRecebidaMilesimos();
            if (quantity <= 0 || quantity > pending)
                throw new BusinessException("Quantidade recebida deve ser positiva e não pode exceder o saldo pendente.", HttpStatus.BAD_REQUEST);
            long subtotal = BigDecimal.valueOf(pedidoItem.getValorUnitarioCentavos()).multiply(BigDecimal.valueOf(quantity))
                    .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).longValueExact();
            ItemEntrada received = new ItemEntrada();
            received.setEntrada(entrada);
            received.setItemPedido(pedidoItem);
            received.setQuantidadeRecebidaMilesimos(quantity);
            received.setValorUnitarioCentavos(pedidoItem.getValorUnitarioCentavos());
            received.setValorTotalCentavos(subtotal);
            entrada.getItens().add(received);
            total = Math.addExact(total, subtotal);
        }
        if (total <= 0) throw new BusinessException("O valor da entrada deve ser maior que zero.", HttpStatus.BAD_REQUEST);
        entrada.setValorTotalCentavos(total);
        EntradaEstoque saved = entradas.saveAndFlush(entrada);
        for (ItemEntrada line : saved.getItens()) {
            ItemPedido orderLine = line.getItemPedido();
            orderLine.setQuantidadeRecebidaMilesimos(Math.addExact(orderLine.getQuantidadeRecebidaMilesimos(), line.getQuantidadeRecebidaMilesimos()));
            Produto product = orderLine.getProduto();
            SaldoEstoque balance = saldos.findByProdutoId(product.getId()).orElseGet(() -> {
                SaldoEstoque created = new SaldoEstoque();
                created.setProduto(product);
                created.setQuantidadeAtualMilesimos(0);
                return created;
            });
            long before = balance.getQuantidadeAtualMilesimos();
            long after = Math.addExact(before, line.getQuantidadeRecebidaMilesimos());
            balance.setQuantidadeAtualMilesimos(after);
            balance.setAtualizadoEm(LocalDateTime.now());
            saldos.save(balance);
            MovimentacaoEstoque movement = new MovimentacaoEstoque();
            movement.setProduto(product);
            movement.setEntrada(saved);
            movement.setQuantidadeMilesimos(line.getQuantidadeRecebidaMilesimos());
            movement.setSaldoAnteriorMilesimos(before);
            movement.setSaldoPosteriorMilesimos(after);
            movement.setValorMovimentacaoCentavos(line.getValorTotalCentavos());
            movement.setDataMovimentacao(LocalDateTime.now());
            movimentacoes.save(movement);
        }
        boolean complete = pedido.getItens().stream().allMatch(i -> i.getQuantidadeRecebidaMilesimos() == i.getQuantidadeSolicitadaMilesimos());
        pedido.setStatus(complete ? StatusPedido.RECEBIDO : StatusPedido.PARCIALMENTE_RECEBIDO);
        pedido.setAtualizadoEm(LocalDateTime.now());
        pedidos.save(pedido);

        TituloPagar title = new TituloPagar();
        title.setNumero(NumeroDocumento.gerar("TP-"));
        title.setFornecedor(pedido.getFornecedor());
        title.setPedido(pedido);
        title.setEntrada(saved);
        title.setDataEmissao(request.dataEntrada());
        title.setDataVencimento(request.dataEntrada().plusDays(pedido.getPrazoPagamentoDias()));
        title.setValorOriginalCentavos(total);
        title.setValorPagoCentavos(0);
        title.setSaldoAbertoCentavos(total);
        title.setStatus(StatusTitulo.EM_ABERTO);
        title.setCriadoEm(LocalDateTime.now());
        title.setAtualizadoEm(LocalDateTime.now());
        titulos.save(title);
        log.info("Entrada {} confirmada para o pedido {}; título {} criado", saved.getNumero(), pedido.getNumero(), title.getNumero());
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<ItemPedidoResponse> pendencias(Long pedidoId) {
        PedidoCompra pedido = pedidos.findById(Objects.requireNonNull(pedidoId)).orElseThrow(() -> new ResourceNotFoundException("Pedido"));
        return pedido.getItens().stream().filter(i -> i.getQuantidadeRecebidaMilesimos() < i.getQuantidadeSolicitadaMilesimos())
                .map(i -> new ItemPedidoResponse(i.getId(), i.getProduto().getId(), i.getProduto().getCodigo(),
                        i.getProduto().getDescricao(), i.getProduto().getUnidadeMedida(),
                        MoneyUtils.fromThousandths(i.getQuantidadeSolicitadaMilesimos()),
                        MoneyUtils.fromThousandths(i.getQuantidadeRecebidaMilesimos()),
                        MoneyUtils.fromThousandths(i.getQuantidadeSolicitadaMilesimos() - i.getQuantidadeRecebidaMilesimos()),
                        MoneyUtils.fromCents(i.getValorUnitarioCentavos()), MoneyUtils.fromCents(i.getValorTotalCentavos()))).toList();
    }

    private EntradaResponse response(EntradaEstoque e) {
        TituloPagar title = titulos.findByEntradaId(e.getId()).orElse(null);
        return new EntradaResponse(e.getId(), e.getNumero(), e.getPedido().getId(), e.getDataEntrada(),
                MoneyUtils.fromCents(e.getValorTotalCentavos()), title == null ? null : title.getId(),
                title == null ? null : title.getNumero(), e.getItens().stream().map(item ->
                new EntradaItemResponse(item.getItemPedido().getId(), item.getItemPedido().getProduto().getCodigo(),
                        item.getItemPedido().getProduto().getDescricao(),
                        MoneyUtils.fromThousandths(item.getQuantidadeRecebidaMilesimos()),
                        MoneyUtils.fromCents(item.getValorUnitarioCentavos()),
                        MoneyUtils.fromCents(item.getValorTotalCentavos()))).toList());
    }
}
