package br.com.gestao.repository;

import br.com.gestao.entity.BaixaTitulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BaixaTituloRepository extends JpaRepository<BaixaTitulo, Long> {
    List<BaixaTitulo> findByTituloIdOrderByDataPagamentoDesc(Long tituloId);
    Optional<BaixaTitulo> findByChaveIdempotencia(String key);
}
