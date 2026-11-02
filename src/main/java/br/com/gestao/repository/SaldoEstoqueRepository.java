package br.com.gestao.repository;

import br.com.gestao.entity.SaldoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SaldoEstoqueRepository extends JpaRepository<SaldoEstoque, Long> {
    Optional<SaldoEstoque> findByProdutoId(Long produtoId);
}
