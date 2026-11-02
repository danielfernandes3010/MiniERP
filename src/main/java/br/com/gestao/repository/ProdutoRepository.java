package br.com.gestao.repository;

import br.com.gestao.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
    long countByAtivoTrue();
    @Query("select p from Produto p where (:q is null or lower(p.descricao) like lower(concat('%',:q,'%')) or lower(p.codigo) like lower(concat('%',:q,'%'))) order by p.descricao")
    List<Produto> pesquisar(@Param("q") String query);
    @Query("select p from Produto p where (:q is null or lower(p.descricao) like lower(concat('%',:q,'%')) or lower(p.codigo) like lower(concat('%',:q,'%'))) and (:categoria is null or lower(p.categoria) = lower(:categoria)) order by p.descricao")
    List<Produto> pesquisarPorCategoria(@Param("q") String query, @Param("categoria") String categoria);
}
