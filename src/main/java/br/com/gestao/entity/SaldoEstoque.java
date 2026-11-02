package br.com.gestao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "saldos_estoque")
@Getter @Setter @NoArgsConstructor
public class SaldoEstoque {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false, unique = true)
    private Produto produto;
    @Column(name = "quantidade_atual_milesimos", nullable = false)
    private long quantidadeAtualMilesimos;
    @Version
    @Column(nullable = false)
    private long versao;
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();
}
