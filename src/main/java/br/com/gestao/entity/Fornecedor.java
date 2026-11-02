package br.com.gestao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "fornecedores")
@Getter @Setter @NoArgsConstructor
public class Fornecedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;
    @Column(name = "razao_social", nullable = false, length = 160)
    private String razaoSocial;
    @Column(name = "nome_fantasia", length = 160)
    private String nomeFantasia;
    @Column(name = "cpf_cnpj", nullable = false, unique = true, length = 14)
    private String cpfCnpj;
    @Column(nullable = false, length = 160)
    private String email;
    private String telefone;
    private String endereco;
    private String cidade;
    private String uf;
    private String cep;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
