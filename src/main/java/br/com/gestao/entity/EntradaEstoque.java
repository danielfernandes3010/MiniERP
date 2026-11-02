package br.com.gestao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entradas_estoque")
@Getter @Setter @NoArgsConstructor
public class EntradaEstoque {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String numero;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoCompra pedido;
    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;
    @Column(name = "valor_total_centavos", nullable = false)
    private long valorTotalCentavos;
    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 100)
    private String chaveIdempotencia;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    @OneToMany(mappedBy = "entrada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemEntrada> itens = new ArrayList<>();
}
