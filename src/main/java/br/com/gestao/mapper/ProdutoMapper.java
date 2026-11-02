package br.com.gestao.mapper;

import br.com.gestao.dto.ApiDtos.ProdutoResponse;
import br.com.gestao.entity.Produto;
import br.com.gestao.util.MoneyUtils;

public final class ProdutoMapper {
    private ProdutoMapper() {}

    public static ProdutoResponse toResponse(Produto produto, long saldoAtualMilesimos) {
        return new ProdutoResponse(produto.getId(), produto.getCodigo(), produto.getDescricao(), produto.getCategoria(),
                produto.getUnidadeMedida(), MoneyUtils.fromCents(produto.getPrecoReferenciaCentavos()),
                MoneyUtils.fromThousandths(produto.getEstoqueMinimoMilesimos()), produto.isAtivo(),
                MoneyUtils.fromThousandths(saldoAtualMilesimos));
    }
}
