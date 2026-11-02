package br.com.gestao.repository;

import br.com.gestao.entity.EntradaEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EntradaEstoqueRepository extends JpaRepository<EntradaEstoque, Long> {
    Optional<EntradaEstoque> findByChaveIdempotencia(String key);
    List<EntradaEstoque> findAllByOrderByCriadoEmDesc();
    List<EntradaEstoque> findByPedidoIdOrderByCriadoEmDesc(Long pedidoId);
}
