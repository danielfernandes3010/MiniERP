package br.com.gestao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "itens_pedido", uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "produto_id"}))
@Getter @Setter @NoArgsConstructor
public class ItemPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoCompra pedido;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;
    @Column(name = "quantidade_solicitada_milesimos", nullable = false)
    private long quantidadeSolicitadaMilesimos;
    @Column(name = "quantidade_recebida_milesimos", nullable = false)
    private long quantidadeRecebidaMilesimos;
    @Column(name = "valor_unitario_centavos", nullable = false)
    private long valorUnitarioCentavos;
    @Column(name = "valor_total_centavos", nullable = false)
    private long valorTotalCentavos;
}
