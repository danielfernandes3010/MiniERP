package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.entity.Produto;
import br.com.gestao.entity.SaldoEstoque;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.exception.ResourceNotFoundException;
import br.com.gestao.repository.ItemPedidoRepository;
import br.com.gestao.repository.ProdutoRepository;
import br.com.gestao.repository.SaldoEstoqueRepository;
import br.com.gestao.mapper.ProdutoMapper;
import br.com.gestao.util.MoneyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ProdutoService {
    private final ProdutoRepository repository;
    private final SaldoEstoqueRepository saldos;
    private final ItemPedidoRepository itensPedido;

    public ProdutoService(ProdutoRepository repository, SaldoEstoqueRepository saldos, ItemPedidoRepository itensPedido) {
        this.repository = repository;
        this.saldos = saldos;
        this.itensPedido = itensPedido;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar(String query, String category) {
        String q = query == null || query.isBlank() ? null : query.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        return repository.pesquisarPorCategoria(q, normalizedCategory).stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public ProdutoResponse obter(@NonNull Long id) { return response(find(id)); }
    @Transactional
    public ProdutoResponse criar(ProdutoRequest r) {
        Produto p = new Produto();
        update(p, r, true);
        Produto saved = repository.save(p);
        SaldoEstoque saldo = new SaldoEstoque();
        saldo.setProduto(saved);
        saldo.setQuantidadeAtualMilesimos(0);
        saldo.setAtualizadoEm(LocalDateTime.now());
        saldos.save(saldo);
        return response(saved);
    }
    @Transactional
    public ProdutoResponse atualizar(@NonNull Long id, ProdutoRequest r) {
        Produto p = find(id);
        update(p, r, false);
        return response(repository.save(Objects.requireNonNull(p)));
    }
    @Transactional
    public void ativar(@NonNull Long id, boolean ativo) {
        Produto p = find(id);
        p.setAtivo(ativo);
        p.setAtualizadoEm(LocalDateTime.now());
    }
    private void update(Produto p, ProdutoRequest r, boolean creating) {
        if (r.precoReferencia().signum() < 0 || r.estoqueMinimo().signum() < 0)
            throw new BusinessException("Preço e estoque mínimo não podem ser negativos.", HttpStatus.BAD_REQUEST);
        boolean duplicate = repository.findAll().stream().anyMatch(x -> !x.getId().equals(p.getId()) && x.getCodigo().equalsIgnoreCase(r.codigo().trim()));
        if (duplicate) throw new BusinessException("Código de produto já cadastrado.");
        if (!creating && !p.getCodigo().equalsIgnoreCase(r.codigo().trim()) && itensPedido.existsByProdutoId(Objects.requireNonNull(idOf(p))))
            throw new BusinessException("Não é possível alterar o código de um produto com histórico de pedidos.");
        p.setCodigo(r.codigo().trim());
        p.setDescricao(r.descricao().trim());
        p.setCategoria(r.categoria() == null || r.categoria().isBlank() ? null : r.categoria().trim());
        p.setUnidadeMedida(r.unidadeMedida());
        p.setPrecoReferenciaCentavos(MoneyUtils.toCents(r.precoReferencia()));
        p.setEstoqueMinimoMilesimos(MoneyUtils.toThousandths(r.estoqueMinimo()));
        if (creating) { p.setAtivo(true); p.setCriadoEm(LocalDateTime.now()); }
        p.setAtualizadoEm(LocalDateTime.now());
    }
    private Long idOf(Produto p) { return p.getId(); }
    private Produto find(@NonNull Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Produto")); }
    private ProdutoResponse response(Produto p) {
        long balance = saldos.findByProdutoId(Objects.requireNonNull(p.getId())).map(saldo -> saldo.getQuantidadeAtualMilesimos()).orElse(0L);
        return ProdutoMapper.toResponse(p, balance);
    }
}
