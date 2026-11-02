package br.com.gestao.repository;

import br.com.gestao.entity.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCpfCnpj(String cpfCnpj);
    long countByAtivoTrue();
    @Query("select f from Fornecedor f where (:q is null or lower(f.razaoSocial) like lower(concat('%',:q,'%')) or lower(f.codigo) like lower(concat('%',:q,'%')) or f.cpfCnpj like concat('%',:q,'%')) order by f.razaoSocial")
    List<Fornecedor> pesquisar(@Param("q") String query);
}
