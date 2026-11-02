package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.entity.TituloPagar;
import br.com.gestao.enums.StatusPedido;
import br.com.gestao.enums.StatusTitulo;
import br.com.gestao.repository.*;
import br.com.gestao.mapper.FinanceiroMapper;
import br.com.gestao.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RelatorioService {
    private final FornecedorRepository fornecedores;
    private final ProdutoRepository produtos;
    private final PedidoCompraRepository pedidos;
    private final TituloPagarRepository titulos;
    private final EstoqueService estoque;

    public RelatorioService(FornecedorRepository fornecedores, ProdutoRepository produtos, PedidoCompraRepository pedidos,
            TituloPagarRepository titulos, EstoqueService estoque) {
        this.fornecedores = fornecedores; this.produtos = produtos; this.pedidos = pedidos;
        this.titulos = titulos; this.estoque = estoque;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo() {
        List<TituloPagar> allTitles = titulos.findAll();
        long lowStock = estoque.listar(null, null, true).stream().filter(e -> !e.situacao().equals("SEM ESTOQUE")).count();
        long noStock = estoque.listar(null, null, true).stream().filter(e -> e.situacao().equals("SEM ESTOQUE")).count();
        long openCents = 0;
        long overdueCents = 0;
        for (TituloPagar nullableTitle : allTitles) {
            TituloPagar title = Objects.requireNonNull(nullableTitle);
            openCents = Math.addExact(openCents, title.getSaldoAbertoCentavos());
            if (title.getSaldoAbertoCentavos() > 0 && title.getDataVencimento().isBefore(LocalDate.now())) {
                overdueCents = Math.addExact(overdueCents, title.getSaldoAbertoCentavos());
            }
        }
        BigDecimal open = MoneyUtils.fromCents(openCents);
        BigDecimal overdue = MoneyUtils.fromCents(overdueCents);
        return new DashboardResponse(fornecedores.countByAtivoTrue(), produtos.countByAtivoTrue(),
                pedidos.countByStatus(StatusPedido.EM_ABERTO), pedidos.countByStatus(StatusPedido.PARCIALMENTE_RECEBIDO),
                lowStock + noStock, open, overdue, titulos.countByStatus(StatusTitulo.QUITADO));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> financeiro(LocalDate dataInicio, LocalDate dataFim, Long fornecedorId,
                                          StatusTitulo status, boolean vencidos, boolean somenteEmAberto) {
        LocalDate today = LocalDate.now();
        List<TituloResponse> rows = titulos.findAllByOrderByDataVencimentoAsc().stream()
                .filter(t -> dataInicio == null || !t.getDataVencimento().isBefore(dataInicio))
                .filter(t -> dataFim == null || !t.getDataVencimento().isAfter(dataFim))
                .filter(t -> fornecedorId == null || t.getFornecedor().getId().equals(fornecedorId))
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> !vencidos || (t.getSaldoAbertoCentavos() > 0 && t.getDataVencimento().isBefore(today)))
                .filter(t -> !somenteEmAberto || t.getSaldoAbertoCentavos() > 0)
                .map(FinanceiroMapper::toResponse).toList();
        BigDecimal open = BigDecimal.ZERO;
        BigDecimal late = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (TituloResponse nullableRow : rows) {
            TituloResponse row = Objects.requireNonNull(nullableRow);
            open = open.add(Objects.requireNonNull(row.saldoAberto()));
            paid = paid.add(Objects.requireNonNull(row.valorPago()));
            if (row.vencido()) late = late.add(Objects.requireNonNull(row.saldoAberto()));
        }
        return Map.of("titulos", rows, "totalEmAberto", open, "totalVencido", late, "totalPago", paid);
    }
}
