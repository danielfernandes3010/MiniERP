package br.com.gestao.controller;

import br.com.gestao.dto.ApiDtos.PedidoRequest;
import br.com.gestao.dto.ApiDtos.PedidoResponse;
import br.com.gestao.dto.ApiDtos.EntradaResponse;
import br.com.gestao.dto.ApiDtos.ItemPedidoResponse;
import br.com.gestao.service.EntradaEstoqueService;
import br.com.gestao.service.PedidoCompraService;
import br.com.gestao.enums.StatusPedido;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoCompraController {
    private final PedidoCompraService service;
    private final EntradaEstoqueService entradas;
    public PedidoCompraController(PedidoCompraService service, EntradaEstoqueService entradas) { this.service = service; this.entradas = entradas; }
    @GetMapping public List<PedidoResponse> listar(@RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) StatusPedido status) { return service.listar(fornecedorId, status); }
    @GetMapping("/{id}") public PedidoResponse obter(@PathVariable @NonNull Long id) { return service.obter(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody PedidoRequest request) { return service.criar(request); }
    @PutMapping("/{id}")
    public PedidoResponse atualizar(@PathVariable @NonNull Long id, @Valid @RequestBody PedidoRequest request) { return service.atualizar(id, request); }
    @PostMapping("/{id}/confirmar")
    public PedidoResponse confirmar(@PathVariable @NonNull Long id) { return service.confirmar(id); }
    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable @NonNull Long id) { return service.cancelar(id); }
    @GetMapping("/{id}/entradas")
    public List<EntradaResponse> entradas(@PathVariable @NonNull Long id) { return entradas.porPedido(id); }
    @GetMapping("/{id}/pendencias")
    public List<ItemPedidoResponse> pendencias(@PathVariable @NonNull Long id) { return entradas.pendencias(id); }
}
