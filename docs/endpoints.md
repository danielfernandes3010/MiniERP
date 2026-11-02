# Endpoints da API REST

Base local: `http://localhost:8080/api`. Os DTOs são JSON; valores monetários são representados em reais nas respostas/requests e armazenados como centavos inteiros. Quantidades de produtos são expressas com até três casas decimais.

## Fornecedores

| Método e caminho | Descrição | Dados/retorno |
|---|---|---|
| `GET /fornecedores?q=` | Lista e pesquisa por texto | Query `q` opcional; lista de `FornecedorResponse` |
| `GET /fornecedores/{id}` | Consulta fornecedor | `FornecedorResponse` |
| `GET /fornecedores/{id}/pedidos` | Lista pedidos do fornecedor | Lista de `PedidoResponse` |
| `POST /fornecedores` | Cadastra fornecedor | `FornecedorRequest`; HTTP 201 e `FornecedorResponse` |
| `PUT /fornecedores/{id}` | Atualiza cadastro | `FornecedorRequest`; `FornecedorResponse` |
| `PATCH /fornecedores/{id}/inativar` | Inativa cadastro | `FornecedorResponse` |
| `PATCH /fornecedores/{id}/reativar` | Reativa cadastro | `FornecedorResponse` |

`FornecedorRequest` contém `codigo`, `razaoSocial`, `nomeFantasia`, `cpfCnpj`, `email`, `telefone`, `endereco`, `cidade`, `uf` e `cep`. Campos obrigatórios: código, razão social, CPF/CNPJ e e-mail.

```json
{
  "codigo": "F-001",
  "razaoSocial": "Fornecedor Exemplo Ltda",
  "nomeFantasia": "Fornecedor Exemplo",
  "cpfCnpj": "529.982.247-25",
  "email": "compras@exemplo.com",
  "telefone": "(11) 3333-4444",
  "endereco": "Rua Um, 100",
  "cidade": "São Paulo",
  "uf": "SP",
  "cep": "01001000"
}
```

Exemplo de resposta:

```json
{
  "id": 1,
  "codigo": "F-001",
  "razaoSocial": "Fornecedor Exemplo Ltda",
  "nomeFantasia": "Fornecedor Exemplo",
  "cpfCnpj": "529.982.247-25",
  "email": "compras@exemplo.com",
  "telefone": "(11) 3333-4444",
  "endereco": "Rua Um, 100",
  "cidade": "São Paulo",
  "uf": "SP",
  "cep": "01001000",
  "ativo": true,
  "criadoEm": "2026-01-15T10:00:00"
}
```

## Produtos

| Método e caminho | Descrição | Dados/retorno |
|---|---|---|
| `GET /produtos?q=&categoria=` | Lista e filtra produtos | Lista de `ProdutoResponse` |
| `GET /produtos/{id}` | Consulta produto | `ProdutoResponse` |
| `POST /produtos` | Cadastra produto | `ProdutoRequest`; HTTP 201 e `ProdutoResponse` |
| `PUT /produtos/{id}` | Atualiza produto | `ProdutoRequest`; `ProdutoResponse` |
| `PATCH /produtos/{id}/inativar` | Inativa produto | HTTP 204, sem corpo |
| `PATCH /produtos/{id}/reativar` | Reativa produto | HTTP 204, sem corpo |

`ProdutoRequest`: `codigo`, `descricao`, `categoria`, `unidadeMedida`, `precoReferencia` e `estoqueMinimo`.

```json
{
  "codigo": "P-001",
  "descricao": "Material de escritório",
  "categoria": "Escritório",
  "unidadeMedida": "UN",
  "precoReferencia": 25.00,
  "estoqueMinimo": 5.000
}
```

Exemplo de resposta: `{"id":1,"codigo":"P-001","descricao":"Material de escritório","categoria":"Escritório","unidadeMedida":"UN","precoReferencia":25.00,"estoqueMinimo":5.000,"ativo":true,"saldoAtual":0.000}`.

## Compras e recebimentos

| Método e caminho | Descrição | Dados/retorno |
|---|---|---|
| `GET /pedidos?fornecedorId=&status=` | Lista pedidos com filtros opcionais | Lista de `PedidoResponse` |
| `GET /pedidos/{id}` | Consulta pedido | `PedidoResponse` |
| `POST /pedidos` | Cria pedido em rascunho | `PedidoRequest`; HTTP 201 e `PedidoResponse` |
| `PUT /pedidos/{id}` | Edita pedido em rascunho | `PedidoRequest`; `PedidoResponse` |
| `POST /pedidos/{id}/confirmar` | Confirma pedido | `PedidoResponse` |
| `POST /pedidos/{id}/cancelar` | Cancela pedido elegível | `PedidoResponse` |
| `GET /pedidos/{id}/entradas` | Lista recebimentos do pedido | Lista de `EntradaResponse` |
| `GET /pedidos/{id}/pendencias` | Consulta itens ainda pendentes | Lista de `ItemPedidoResponse` |
| `GET /entradas` | Lista entradas registradas | Lista de `EntradaResponse` |
| `GET /entradas/{id}` | Consulta entrada | `EntradaResponse` |
| `POST /entradas` | Registra recebimento | `EntradaRequest`; HTTP 201 e `EntradaResponse` |

`PedidoRequest` requer `fornecedorId`, `prazoPagamentoDias` e pelo menos um item em `itens`. Cada item contém `produtoId`, `quantidade` e `valorUnitario`.

```json
{
  "fornecedorId": 1,
  "previsaoEntrega": "2027-06-30",
  "prazoPagamentoDias": 30,
  "observacoes": "Entrega na unidade central",
  "itens": [
    {"produtoId": 1, "quantidade": 10.000, "valorUnitario": 25.00}
  ]
}
```

Exemplo de resposta:

```json
{
  "id": 1,
  "numero": "PC-A1B2C3D4E5F6",
  "fornecedorId": 1,
  "fornecedor": "Fornecedor Exemplo Ltda",
  "dataEmissao": "2026-11-01",
  "previsaoEntrega": "2027-06-30",
  "prazoPagamentoDias": 30,
  "valorTotal": 250.00,
  "status": "RASCUNHO",
  "observacoes": "Entrega na unidade central",
  "itens": [
    {
      "id": 1,
      "produtoId": 1,
      "codigoProduto": "P-001",
      "descricaoProduto": "Material de escritório",
      "unidadeMedida": "UN",
      "quantidadeSolicitada": 10.000,
      "quantidadeRecebida": 0.000,
      "quantidadePendente": 10.000,
      "valorUnitario": 25.00,
      "valorTotal": 250.00
    }
  ]
}
```

Os status usados no domínio incluem rascunho, em aberto, parcialmente recebido, recebido e cancelado.

Para registrar recebimento, `EntradaRequest` requer `pedidoId`, `dataEntrada`, `chaveIdempotencia` e uma lista `itens` com `itemPedidoId` e `quantidadeRecebida`.

```json
{
  "pedidoId": 1,
  "dataEntrada": "2026-11-01",
  "chaveIdempotencia": "recebimento-pedido-1-lote-1",
  "itens": [{"itemPedidoId": 1, "quantidadeRecebida": 4.000}]
}
```

Exemplo de resposta:

```json
{
  "id": 1,
  "numero": "EN-A1B2C3D4E5F6",
  "pedidoId": 1,
  "dataEntrada": "2026-11-01",
  "valorTotal": 100.00,
  "tituloId": 1,
  "numeroTitulo": "TP-B2C3D4E5F6A1",
  "itens": [
    {
      "itemPedidoId": 1,
      "codigoProduto": "P-001",
      "descricaoProduto": "Material de escritório",
      "quantidadeRecebida": 4.000,
      "valorUnitario": 25.00,
      "valorTotal": 100.00
    }
  ]
}
```

## Estoque

| Método e caminho | Descrição | Dados/retorno |
|---|---|---|
| `GET /estoque?q=&categoria=&abaixoMinimo=` | Lista saldos; filtros são opcionais | Lista de `EstoqueResponse`; `abaixoMinimo` padrão `false` |
| `GET /estoque/{produtoId}` | Consulta saldo do produto | `EstoqueResponse` |
| `GET /estoque/{produtoId}/movimentacoes` | Lista histórico de movimentações | Lista de movimentações |

## Financeiro

| Método e caminho | Descrição | Dados/retorno |
|---|---|---|
| `GET /titulos?fornecedorId=&status=&vencidos=&dataInicio=&dataFim=` | Lista títulos e aplica filtros opcionais | Lista de `TituloResponse` |
| `GET /titulos/{id}` | Consulta título | `TituloResponse` |
| `POST /titulos/{id}/baixas` | Registra pagamento | `BaixaRequest`; HTTP 201 e `TituloResponse` atualizado |
| `GET /titulos/{id}/baixas` | Consulta histórico de pagamentos | Lista de `BaixaResponse` |

`BaixaRequest` requer `dataPagamento`, `valorPago`, `formaPagamento` e `chaveIdempotencia`; `observacoes` é opcional.

```json
{
  "dataPagamento": "2026-11-02",
  "valorPago": 40.00,
  "formaPagamento": "PIX",
  "observacoes": "Pagamento parcial",
  "chaveIdempotencia": "baixa-titulo-1-01"
}
```

Exemplo da resposta atualizada do título:

```json
{
  "id": 1,
  "numero": "TP-B2C3D4E5F6A1",
  "fornecedorId": 1,
  "fornecedor": "Fornecedor Exemplo Ltda",
  "pedidoId": 1,
  "pedido": "PC-A1B2C3D4E5F6",
  "entradaId": 1,
  "dataEmissao": "2026-11-01",
  "dataVencimento": "2026-12-01",
  "valorOriginal": 100.00,
  "valorPago": 40.00,
  "saldoAberto": 60.00,
  "status": "PARCIALMENTE_PAGO",
  "vencido": false
}
```

## Relatórios

| Método e caminho | Descrição | Filtros/retorno |
|---|---|---|
| `GET /relatorios/resumo` | Indicadores do sistema | `DashboardResponse` |
| `GET /relatorios/estoque?q=&categoria=&abaixoMinimo=` | Relatório de estoque | mesmos filtros da consulta de estoque |
| `GET /relatorios/contas-pagar?dataInicio=&dataFim=&fornecedorId=&status=&vencidos=&somenteEmAberto=` | Relatório de títulos | filtros opcionais; retorna `titulos`, `totalEmAberto`, `totalVencido` e `totalPago` |

`GET /relatorios/resumo` retorna `fornecedoresAtivos`, `produtosAtivos`, `pedidosEmAberto`, `pedidosParciais`, `produtosEstoqueBaixo`, `totalFinanceiroAberto`, `totalFinanceiroVencido` e `titulosQuitados`.

## Erros e códigos HTTP

Respostas de erro usam o formato:

```json
{
  "timestamp": "2026-06-01T10:00:00Z",
  "status": 400,
  "message": "Revise os campos informados.",
  "path": "/api/pedidos",
  "fieldErrors": {"itens": "must not be empty"}
}
```

- **400 Bad Request:** JSON inválido ou campos não válidos; `fieldErrors` pode indicar os campos.
- **404 Not Found:** recurso solicitado não encontrado.
- **409 Conflict:** transição de negócio não permitida, duplicidade ou violação de integridade.
- **500 Internal Server Error:** falha inesperada; a resposta não expõe detalhes internos.
- **201 Created:** cadastro, pedido, entrada ou baixa criada.
- **204 No Content:** inativação/reativação de produto.
