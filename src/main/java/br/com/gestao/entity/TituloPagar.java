package br.com.gestao.entity;

import br.com.gestao.enums.StatusTitulo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "titulos_pagar")
@Getter @Setter @NoArgsConstructor
public class TituloPagar {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String numero;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoCompra pedido;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id", nullable = false, unique = true)
    private EntradaEstoque entrada;
    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;
    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;
    @Column(name = "valor_original_centavos", nullable = false)
    private long valorOriginalCentavos;
    @Column(name = "valor_pago_centavos", nullable = false)
    private long valorPagoCentavos;
    @Column(name = "saldo_aberto_centavos", nullable = false)
    private long saldoAbertoCentavos;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StatusTitulo status = StatusTitulo.EM_ABERTO;
    private String observacoes;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
