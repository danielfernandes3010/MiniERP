package br.com.gestao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoque")
@Getter @Setter @NoArgsConstructor
public class MovimentacaoEstoque {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id", nullable = false)
    private EntradaEstoque entrada;
    @Column(name = "tipo_movimentacao", nullable = false, length = 30)
    private String tipoMovimentacao = "ENTRADA_COMPRA";
    @Column(name = "quantidade_milesimos", nullable = false)
    private long quantidadeMilesimos;
    @Column(name = "saldo_anterior_milesimos", nullable = false)
    private long saldoAnteriorMilesimos;
    @Column(name = "saldo_posterior_milesimos", nullable = false)
    private long saldoPosteriorMilesimos;
    @Column(name = "valor_movimentacao_centavos", nullable = false)
    private long valorMovimentacaoCentavos;
    @Column(name = "data_movimentacao", nullable = false)
    private LocalDateTime dataMovimentacao;
}
