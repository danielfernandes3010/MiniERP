package br.com.gestao.controller;

import br.com.gestao.dto.ApiDtos.DashboardResponse;
import br.com.gestao.dto.ApiDtos.EstoqueResponse;
import br.com.gestao.service.EstoqueService;
import br.com.gestao.service.RelatorioService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import br.com.gestao.enums.StatusTitulo;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {
    private final RelatorioService relatorios;
    private final EstoqueService estoque;
    public RelatorioController(RelatorioService relatorios, EstoqueService estoque) {
        this.relatorios = relatorios; this.estoque = estoque;
    }
    @GetMapping("/resumo") public DashboardResponse resumo() { return relatorios.resumo(); }
    @GetMapping("/estoque") public List<EstoqueResponse> estoque(@RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria, @RequestParam(defaultValue = "false") boolean abaixoMinimo) {
        return estoque.listar(q, categoria, abaixoMinimo);
    }
    @GetMapping("/contas-pagar") public Map<String, Object> financeiro(
            @RequestParam(required = false) LocalDate dataInicio, @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) Long fornecedorId, @RequestParam(required = false) StatusTitulo status,
            @RequestParam(defaultValue = "false") boolean vencidos, @RequestParam(defaultValue = "false") boolean somenteEmAberto) {
        return relatorios.financeiro(dataInicio, dataFim, fornecedorId, status, vencidos, somenteEmAberto);
    }
}
