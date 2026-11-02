package br.com.gestao.controller;

import br.com.gestao.dto.ApiDtos.EntradaRequest;
import br.com.gestao.dto.ApiDtos.EntradaResponse;
import br.com.gestao.service.EntradaEstoqueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/entradas")
public class EntradaEstoqueController {
    private final EntradaEstoqueService service;
    public EntradaEstoqueController(EntradaEstoqueService service) { this.service = service; }
    @GetMapping public List<EntradaResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") public EntradaResponse obter(@PathVariable @NonNull Long id) { return service.obter(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public EntradaResponse registrar(@Valid @RequestBody EntradaRequest request) { return service.registrar(request); }
}
