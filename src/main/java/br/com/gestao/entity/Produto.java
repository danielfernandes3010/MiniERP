package br.com.gestao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "produtos")
@Getter @Setter @NoArgsConstructor
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;
    @Column(nullable = false, length = 180)
    private String descricao;
    private String categoria;
    @Column(name = "unidade_medida", nullable = false, length = 10)
    private String unidadeMedida;
    @Column(name = "preco_referencia_centavos", nullable = false)
    private long precoReferenciaCentavos;
    @Column(name = "estoque_minimo_milesimos", nullable = false)
    private long estoqueMinimoMilesimos;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
