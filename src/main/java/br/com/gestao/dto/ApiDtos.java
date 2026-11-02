package br.com.gestao.dto;

import br.com.gestao.enums.FormaPagamento;
import br.com.gestao.enums.StatusPedido;
import br.com.gestao.enums.StatusTitulo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record FornecedorRequest(
            @NotBlank @Size(max = 30) String codigo,
            @NotBlank @Size(min = 3, max = 160) String razaoSocial,
            @Size(max = 160) String nomeFantasia,
            @NotBlank String cpfCnpj,
            @NotBlank @Email @Size(max = 160) String email,
            String telefone, String endereco, String cidade,
            @Pattern(regexp = "^$|^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$") String uf,
            @Pattern(regexp = "^$|^[0-9]{8}$") String cep) {}

    public record FornecedorResponse(Long id, String codigo, String razaoSocial, String nomeFantasia,
            String cpfCnpj, String email, String telefone, String endereco, String cidade, String uf,
            String cep, boolean ativo, LocalDateTime criadoEm) {}

    public record ProdutoRequest(
            @NotBlank @Size(max = 40) String codigo,
            @NotBlank @Size(max = 180) String descricao,
            String categoria,
            @NotBlank @Pattern(regexp = "UN|KG|L|CX|M|PC") String unidadeMedida,
            @NotNull @Digits(integer = 12, fraction = 2) @DecimalMin(value = "0.00") BigDecimal precoReferencia,
            @NotNull @Digits(integer = 12, fraction = 3) @DecimalMin(value = "0.000") BigDecimal estoqueMinimo) {}

    public record ProdutoResponse(Long id, String codigo, String descricao, String categoria,
            String unidadeMedida, BigDecimal precoReferencia, BigDecimal estoqueMinimo, boolean ativo,
            BigDecimal saldoAtual) {}

    public record ItemPedidoRequest(@NotNull Long produtoId,
            @NotNull @Digits(integer = 12, fraction = 3) @DecimalMin(value = "0.001") BigDecimal quantidade,
            @NotNull @Digits(integer = 12, fraction = 2) @DecimalMin(value = "0.01") BigDecimal valorUnitario) {}

    public record PedidoRequest(@NotNull Long fornecedorId, LocalDate previsaoEntrega,
            @NotNull @Min(0) Integer prazoPagamentoDias, String observacoes,
            @NotEmpty List<@Valid ItemPedidoRequest> itens) {}

    public record ItemPedidoResponse(Long id, Long produtoId, String codigoProduto, String descricaoProduto,
            String unidadeMedida, BigDecimal quantidadeSolicitada, BigDecimal quantidadeRecebida,
            BigDecimal quantidadePendente, BigDecimal valorUnitario, BigDecimal valorTotal) {}

    public record PedidoResponse(Long id, String numero, Long fornecedorId, String fornecedor,
            LocalDate dataEmissao, LocalDate previsaoEntrega, int prazoPagamentoDias, BigDecimal valorTotal,
            StatusPedido status, String observacoes, List<ItemPedidoResponse> itens) {}

    public record RecebimentoItemRequest(@NotNull Long itemPedidoId,
            @NotNull @Digits(integer = 12, fraction = 3) @DecimalMin(value = "0.001") BigDecimal quantidadeRecebida) {}

    public record EntradaRequest(@NotNull Long pedidoId, @NotNull LocalDate dataEntrada,
            @NotBlank String chaveIdempotencia,
            @NotEmpty List<@Valid RecebimentoItemRequest> itens) {}

    public record EntradaItemResponse(Long itemPedidoId, String codigoProduto, String descricaoProduto,
            BigDecimal quantidadeRecebida, BigDecimal valorUnitario, BigDecimal valorTotal) {}

    public record EntradaResponse(Long id, String numero, Long pedidoId, LocalDate dataEntrada,
            BigDecimal valorTotal, Long tituloId, String numeroTitulo, List<EntradaItemResponse> itens) {}

    public record EstoqueResponse(Long produtoId, String codigo, String descricao, String categoria,
            String unidadeMedida, BigDecimal saldoFisico, BigDecimal estoqueMinimo, String situacao,
            LocalDateTime ultimaMovimentacao) {}

    public record BaixaRequest(@NotNull LocalDate dataPagamento,
            @NotNull @Digits(integer = 12, fraction = 2) @DecimalMin(value = "0.01") BigDecimal valorPago,
            @NotNull FormaPagamento formaPagamento, String observacoes,
            @NotBlank String chaveIdempotencia) {}

    public record BaixaResponse(Long id, LocalDate dataPagamento, BigDecimal valorPago,
            FormaPagamento formaPagamento, String observacoes) {}

    public record TituloResponse(Long id, String numero, Long fornecedorId, String fornecedor,
            Long pedidoId, String pedido, Long entradaId, LocalDate dataEmissao,
            LocalDate dataVencimento, BigDecimal valorOriginal, BigDecimal valorPago,
            BigDecimal saldoAberto, StatusTitulo status, boolean vencido) {}

    public record DashboardResponse(long fornecedoresAtivos, long produtosAtivos, long pedidosEmAberto,
            long pedidosParciais, long produtosEstoqueBaixo, BigDecimal totalFinanceiroAberto,
            BigDecimal totalFinanceiroVencido, long titulosQuitados) {}
}
