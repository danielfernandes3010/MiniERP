package br.com.gestao.controller;

import br.com.gestao.dto.ApiDtos.FornecedorRequest;
import br.com.gestao.dto.ApiDtos.FornecedorResponse;
import br.com.gestao.service.FornecedorService;
import br.com.gestao.dto.ApiDtos.PedidoResponse;
import br.com.gestao.service.PedidoCompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {
    private final FornecedorService service;
    private final PedidoCompraService pedidos;
    public FornecedorController(FornecedorService service, PedidoCompraService pedidos) { this.service = service; this.pedidos = pedidos; }
    @GetMapping public List<FornecedorResponse> listar(@RequestParam(required = false) String q) { return service.listar(q); }
    @GetMapping("/{id}") public FornecedorResponse obter(@PathVariable @NonNull Long id) { return service.obter(id); }
    @GetMapping("/{id}/pedidos") public List<PedidoResponse> pedidos(@PathVariable @NonNull Long id) { return pedidos.porFornecedor(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResponse criar(@Valid @RequestBody FornecedorRequest request) { return service.criar(request); }
    @PutMapping("/{id}")
    public FornecedorResponse atualizar(@PathVariable @NonNull Long id, @Valid @RequestBody FornecedorRequest request) { return service.atualizar(id, request); }
    @PatchMapping("/{id}/inativar")
    public FornecedorResponse inativar(@PathVariable @NonNull Long id) { return service.ativar(id, false); }
    @PatchMapping("/{id}/reativar")
    public FornecedorResponse reativar(@PathVariable @NonNull Long id) { return service.ativar(id, true); }
}
