package br.com.gestao.service;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.entity.BaixaTitulo;
import br.com.gestao.entity.TituloPagar;
import br.com.gestao.enums.StatusTitulo;
import br.com.gestao.exception.BusinessException;
import br.com.gestao.exception.ResourceNotFoundException;
import br.com.gestao.repository.BaixaTituloRepository;
import br.com.gestao.repository.TituloPagarRepository;
import br.com.gestao.mapper.FinanceiroMapper;
import br.com.gestao.util.MoneyUtils;
import br.com.gestao.util.SqliteWriteLock;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TituloPagarService {
    private final TituloPagarRepository titulos;
    private final BaixaTituloRepository baixas;

    public TituloPagarService(TituloPagarRepository titulos, BaixaTituloRepository baixas) {
        this.titulos = titulos;
        this.baixas = baixas;
    }

    @Transactional(readOnly = true)
    public List<TituloResponse> listar(Long fornecedorId, StatusTitulo status, Boolean vencidos,
                                       LocalDate dataInicio, LocalDate dataFim) {
        LocalDate today = LocalDate.now();
        return titulos.findAllByOrderByDataVencimentoAsc().stream()
                .filter(t -> fornecedorId == null || t.getFornecedor().getId().equals(fornecedorId))
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> vencidos == null || !vencidos || (t.getSaldoAbertoCentavos() > 0 && t.getDataVencimento().isBefore(today)))
                .filter(t -> dataInicio == null || !t.getDataVencimento().isBefore(dataInicio))
                .filter(t -> dataFim == null || !t.getDataVencimento().isAfter(dataFim))
                .map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public TituloResponse obter(@NonNull Long id) { return response(find(id)); }
    @Transactional(readOnly = true)
    public List<BaixaResponse> historico(@NonNull Long id) {
        find(id);
        return baixas.findByTituloIdOrderByDataPagamentoDesc(id).stream()
                .map(FinanceiroMapper::toResponse).toList();
    }

    @Transactional
    public TituloResponse baixar(@NonNull Long id, BaixaRequest request) {
        SqliteWriteLock.lockUntilTransactionCompletes();
        var existing = baixas.findByChaveIdempotencia(request.chaveIdempotencia().trim());
        if (existing.isPresent()) {
            if (!existing.get().getTitulo().getId().equals(id))
                throw new BusinessException("Chave de idempotência já utilizada em outro título.");
            return response(existing.get().getTitulo());
        }
        TituloPagar title = find(id);
        if (title.getStatus() == StatusTitulo.QUITADO) throw new BusinessException("Título já está quitado.");
        if (request.dataPagamento().isAfter(LocalDate.now()))
            throw new BusinessException("Data do pagamento não pode ser futura.", HttpStatus.BAD_REQUEST);
        long cents = MoneyUtils.toCents(request.valorPago());
        if (cents <= 0 || cents > title.getSaldoAbertoCentavos())
            throw new BusinessException("O pagamento deve ser positivo e não pode superar o saldo em aberto.", HttpStatus.BAD_REQUEST);
        BaixaTitulo payment = new BaixaTitulo();
        payment.setTitulo(title);
        payment.setDataPagamento(request.dataPagamento());
        payment.setValorPagoCentavos(cents);
        payment.setFormaPagamento(request.formaPagamento());
        payment.setObservacoes(request.observacoes());
        payment.setChaveIdempotencia(request.chaveIdempotencia().trim());
        payment.setCriadoEm(LocalDateTime.now());
        baixas.save(payment);
        long paid = Math.addExact(title.getValorPagoCentavos(), cents);
        title.setValorPagoCentavos(paid);
        title.setSaldoAbertoCentavos(title.getValorOriginalCentavos() - paid);
        title.setStatus(title.getSaldoAbertoCentavos() == 0 ? StatusTitulo.QUITADO : StatusTitulo.PARCIALMENTE_PAGO);
        title.setAtualizadoEm(LocalDateTime.now());
        return response(titulos.save(title));
    }

    private TituloPagar find(@NonNull Long id) { return titulos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Título")); }
    private TituloResponse response(TituloPagar t) { return FinanceiroMapper.toResponse(t); }
}
