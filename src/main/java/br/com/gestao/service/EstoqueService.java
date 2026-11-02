package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.EstoqueResponse;
import br.com.gestao.entity.Produto;
import br.com.gestao.entity.SaldoEstoque;
import br.com.gestao.exception.ResourceNotFoundException;
import br.com.gestao.repository.MovimentacaoEstoqueRepository;
import br.com.gestao.repository.ProdutoRepository;
import br.com.gestao.repository.SaldoEstoqueRepository;
import br.com.gestao.util.MoneyUtils;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueService {
    private final ProdutoRepository produtos;
    private final SaldoEstoqueRepository saldos;
    private final MovimentacaoEstoqueRepository movimentacoes;

    public EstoqueService(ProdutoRepository produtos, SaldoEstoqueRepository saldos, MovimentacaoEstoqueRepository movimentacoes) {
        this.produtos = produtos;
        this.saldos = saldos;
        this.movimentacoes = movimentacoes;
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponse> listar(String q, String categoria, boolean abaixoMinimo) {
        String query = q == null || q.isBlank() ? null : q.trim();
        return produtos.pesquisar(query).stream()
                .filter(p -> categoria == null || categoria.isBlank() || categoria.equalsIgnoreCase(p.getCategoria()))
                .map(this::response)
                .filter(e -> !abaixoMinimo || e.saldoFisico().compareTo(e.estoqueMinimo()) <= 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstoqueResponse obter(@NonNull Long produtoId) {
        Produto p = produtos.findById(produtoId).orElseThrow(() -> new ResourceNotFoundException("Produto"));
        return response(p);
    }

    @Transactional(readOnly = true)
    public List<?> movimentacoes(@NonNull Long produtoId) {
        if (!produtos.existsById(produtoId)) throw new ResourceNotFoundException("Produto");
        return movimentacoes.findByProdutoIdOrderByDataMovimentacaoDesc(produtoId).stream()
                .map(m -> java.util.Map.of(
                        "id", m.getId(), "tipo", m.getTipoMovimentacao(),
                        "entradaId", m.getEntrada().getId(), "numeroEntrada", m.getEntrada().getNumero(),
                        "quantidade", MoneyUtils.fromThousandths(m.getQuantidadeMilesimos()),
                        "saldoAnterior", MoneyUtils.fromThousandths(m.getSaldoAnteriorMilesimos()),
                        "saldoPosterior", MoneyUtils.fromThousandths(m.getSaldoPosteriorMilesimos()),
                        "valor", MoneyUtils.fromCents(m.getValorMovimentacaoCentavos()),
                        "data", m.getDataMovimentacao())).toList();
    }

    private EstoqueResponse response(Produto p) {
        SaldoEstoque balance = saldos.findByProdutoId(p.getId()).orElse(null);
        long qty = balance == null ? 0 : balance.getQuantidadeAtualMilesimos();
        String status = qty == 0 ? "SEM ESTOQUE" : qty <= p.getEstoqueMinimoMilesimos() ? "ESTOQUE BAIXO" : "NORMAL";
        var last = movimentacoes.findByProdutoIdOrderByDataMovimentacaoDesc(p.getId()).stream().findFirst().orElse(null);
        return new EstoqueResponse(p.getId(), p.getCodigo(), p.getDescricao(), p.getCategoria(), p.getUnidadeMedida(),
                MoneyUtils.fromThousandths(qty), MoneyUtils.fromThousandths(p.getEstoqueMinimoMilesimos()), status,
                last == null ? null : last.getDataMovimentacao());
    }
}
