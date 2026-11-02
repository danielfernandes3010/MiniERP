package br.com.gestao.controller;

import br.com.gestao.dto.ApiDtos.ProdutoRequest;
import br.com.gestao.dto.ApiDtos.ProdutoResponse;
import br.com.gestao.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {
    private final ProdutoService service;
    public ProdutoController(ProdutoService service) { this.service = service; }
    @GetMapping public List<ProdutoResponse> listar(@RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria) { return service.listar(q, categoria); }
    @GetMapping("/{id}") public ProdutoResponse obter(@PathVariable @NonNull Long id) { return service.obter(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criar(@Valid @RequestBody ProdutoRequest request) { return service.criar(request); }
    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable @NonNull Long id, @Valid @RequestBody ProdutoRequest request) { return service.atualizar(id, request); }
    @PatchMapping("/{id}/inativar") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable @NonNull Long id) { service.ativar(id, false); }
    @PatchMapping("/{id}/reativar") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativar(@PathVariable @NonNull Long id) { service.ativar(id, true); }
}
