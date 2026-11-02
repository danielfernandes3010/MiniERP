package br.com.gestao.entity;

import br.com.gestao.enums.StatusPedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos_compra")
@Getter @Setter @NoArgsConstructor
public class PedidoCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String numero;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;
    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;
    @Column(name = "previsao_entrega")
    private LocalDate previsaoEntrega;
    @Column(name = "prazo_pagamento_dias", nullable = false)
    private int prazoPagamentoDias;
    @Column(name = "valor_total_centavos", nullable = false)
    private long valorTotalCentavos;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.RASCUNHO;
    private String observacoes;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();
}
