package br.com.gestao.repository;

import br.com.gestao.entity.PedidoCompra;
import br.com.gestao.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {
    boolean existsByFornecedorId(Long fornecedorId);
    long countByStatus(br.com.gestao.enums.StatusPedido status);
    List<PedidoCompra> findAllByOrderByCriadoEmDesc();
    List<PedidoCompra> findByStatusInOrderByCriadoEmDesc(List<StatusPedido> status);
    List<PedidoCompra> findByFornecedorIdOrderByCriadoEmDesc(Long fornecedorId);
}
