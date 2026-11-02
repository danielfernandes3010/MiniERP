package br.com.gestao.entity;

import br.com.gestao.enums.FormaPagamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "baixas_titulo")
@Getter @Setter @NoArgsConstructor
public class BaixaTitulo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "titulo_id", nullable = false)
    private TituloPagar titulo;
    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;
    @Column(name = "valor_pago_centavos", nullable = false)
    private long valorPagoCentavos;
    @Enumerated(EnumType.STRING) @Column(name = "forma_pagamento", nullable = false, length = 30)
    private FormaPagamento formaPagamento;
    private String observacoes;
    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 100)
    private String chaveIdempotencia;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
