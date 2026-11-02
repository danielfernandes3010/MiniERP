package br.com.gestao.repository;

import br.com.gestao.entity.TituloPagar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TituloPagarRepository extends JpaRepository<TituloPagar, Long> {
    List<TituloPagar> findAllByOrderByDataVencimentoAsc();
    boolean existsByEntradaId(Long entradaId);
    long countByStatus(br.com.gestao.enums.StatusTitulo status);
    Optional<TituloPagar> findByEntradaId(Long entradaId);
}
