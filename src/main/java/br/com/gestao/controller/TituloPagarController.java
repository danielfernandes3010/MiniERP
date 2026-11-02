package br.com.gestao.controller;

import br.com.gestao.dto.ApiDtos.*;
import br.com.gestao.service.TituloPagarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import br.com.gestao.enums.StatusTitulo;

@RestController
@RequestMapping("/api/titulos")
public class TituloPagarController {
    private final TituloPagarService service;
    public TituloPagarController(TituloPagarService service) { this.service = service; }
    @GetMapping public List<TituloResponse> listar(@RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) StatusTitulo status, @RequestParam(required = false) Boolean vencidos,
            @RequestParam(required = false) LocalDate dataInicio, @RequestParam(required = false) LocalDate dataFim) {
        return service.listar(fornecedorId, status, vencidos, dataInicio, dataFim);
    }
    @GetMapping("/{id}") public TituloResponse obter(@PathVariable @NonNull Long id) { return service.obter(id); }
    @PostMapping("/{id}/baixas") @ResponseStatus(HttpStatus.CREATED)
    public TituloResponse baixar(@PathVariable @NonNull Long id, @Valid @RequestBody BaixaRequest request) { return service.baixar(id, request); }
    @GetMapping("/{id}/baixas") public List<BaixaResponse> historico(@PathVariable @NonNull Long id) { return service.historico(id); }
}
