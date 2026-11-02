package br.com.gestao.controller;

import br.com.gestao.dto.ApiDtos.EstoqueResponse;
import br.com.gestao.service.EstoqueService;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;
import java.util.List;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {
    private final EstoqueService service;
    public EstoqueController(EstoqueService service) { this.service = service; }
    @GetMapping public List<EstoqueResponse> listar(@RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria, @RequestParam(defaultValue = "false") boolean abaixoMinimo) {
        return service.listar(q, categoria, abaixoMinimo);
    }
    @GetMapping("/{produtoId}") public EstoqueResponse obter(@PathVariable @NonNull Long produtoId) { return service.obter(produtoId); }
    @GetMapping("/{produtoId}/movimentacoes") public List<?> movimentacoes(@PathVariable @NonNull Long produtoId) { return service.movimentacoes(produtoId); }
}
