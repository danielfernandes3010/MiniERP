package br.com.gestao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "itens_entrada", uniqueConstraints = @UniqueConstraint(columnNames = {"entrada_id", "item_pedido_id"}))
@Getter @Setter @NoArgsConstructor
public class ItemEntrada {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id", nullable = false)
    private EntradaEstoque entrada;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_pedido_id", nullable = false)
    private ItemPedido itemPedido;
    @Column(name = "quantidade_recebida_milesimos", nullable = false)
    private long quantidadeRecebidaMilesimos;
    @Column(name = "valor_unitario_centavos", nullable = false)
    private long valorUnitarioCentavos;
    @Column(name = "valor_total_centavos", nullable = false)
    private long valorTotalCentavos;
}
