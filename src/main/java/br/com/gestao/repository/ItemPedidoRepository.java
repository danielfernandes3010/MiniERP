package br.com.gestao.repository;

import br.com.gestao.entity.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
    List<ItemPedido> findByPedidoId(Long pedidoId);
    boolean existsByProdutoId(Long produtoId);
}
