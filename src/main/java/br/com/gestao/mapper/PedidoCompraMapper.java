package br.com.gestao.mapper;

import br.com.gestao.dto.ApiDtos.ItemPedidoResponse;
import br.com.gestao.dto.ApiDtos.PedidoResponse;
import br.com.gestao.entity.PedidoCompra;
import br.com.gestao.util.MoneyUtils;

public final class PedidoCompraMapper {
    private PedidoCompraMapper() {}

    public static PedidoResponse toResponse(PedidoCompra pedido) {
        var items = pedido.getItens().stream().map(item -> new ItemPedidoResponse(item.getId(),
                item.getProduto().getId(), item.getProduto().getCodigo(), item.getProduto().getDescricao(),
                item.getProduto().getUnidadeMedida(), MoneyUtils.fromThousandths(item.getQuantidadeSolicitadaMilesimos()),
                MoneyUtils.fromThousandths(item.getQuantidadeRecebidaMilesimos()),
                MoneyUtils.fromThousandths(item.getQuantidadeSolicitadaMilesimos() - item.getQuantidadeRecebidaMilesimos()),
                MoneyUtils.fromCents(item.getValorUnitarioCentavos()), MoneyUtils.fromCents(item.getValorTotalCentavos()))).toList();
        return new PedidoResponse(pedido.getId(), pedido.getNumero(), pedido.getFornecedor().getId(),
                pedido.getFornecedor().getRazaoSocial(), pedido.getDataEmissao(), pedido.getPrevisaoEntrega(),
                pedido.getPrazoPagamentoDias(), MoneyUtils.fromCents(pedido.getValorTotalCentavos()),
                pedido.getStatus(), pedido.getObservacoes(), items);
    }
}
