package br.com.gestao.mapper;

import br.com.gestao.dto.ApiDtos.BaixaResponse;
import br.com.gestao.dto.ApiDtos.TituloResponse;
import br.com.gestao.entity.BaixaTitulo;
import br.com.gestao.entity.TituloPagar;
import br.com.gestao.util.MoneyUtils;

import java.time.LocalDate;

public final class FinanceiroMapper {
    private FinanceiroMapper() {}

    public static TituloResponse toResponse(TituloPagar titulo) {
        return new TituloResponse(titulo.getId(), titulo.getNumero(), titulo.getFornecedor().getId(),
                titulo.getFornecedor().getRazaoSocial(), titulo.getPedido().getId(), titulo.getPedido().getNumero(),
                titulo.getEntrada().getId(), titulo.getDataEmissao(), titulo.getDataVencimento(),
                MoneyUtils.fromCents(titulo.getValorOriginalCentavos()), MoneyUtils.fromCents(titulo.getValorPagoCentavos()),
                MoneyUtils.fromCents(titulo.getSaldoAbertoCentavos()), titulo.getStatus(),
                titulo.getSaldoAbertoCentavos() > 0 && titulo.getDataVencimento().isBefore(LocalDate.now()));
    }

    public static BaixaResponse toResponse(BaixaTitulo baixa) {
        return new BaixaResponse(baixa.getId(), baixa.getDataPagamento(), MoneyUtils.fromCents(baixa.getValorPagoCentavos()),
                baixa.getFormaPagamento(), baixa.getObservacoes());
    }
}
