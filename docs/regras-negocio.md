# Regras de negócio

1. Código do fornecedor/produto e CPF/CNPJ normalizado são únicos.
2. Fornecedor e produto inativos não podem ser usados em pedidos novos.
3. Pedido começa como `RASCUNHO`; só esse status aceita edição e confirmação.
4. Pedido em aberto pode ser cancelado se ainda não recebeu mercadoria; pedido parcial não pode ser cancelado.
5. Cada produto aparece no máximo uma vez por pedido; preço e quantidade precisam ser positivos.
6. Valores de item são arredondados para centavos com HALF_UP e o total é a soma dos itens arredondados.
7. Entrada precisa referenciar pedido confirmado, data não futura e item que pertença ao pedido.
8. A quantidade da entrada não pode exceder o saldo pendente. O recebimento parcial mantém o pedido parcial; o último recebimento marca-o recebido.
9. Entrada altera saldo, cria movimentação e título em uma única transação. Chave de idempotência repetida não processa novamente.
10. Título é único por entrada, vale o valor efetivamente recebido e vence na data de entrada mais o prazo em dias corridos.
11. Pagamentos são históricos e idempotentes. O pagamento não pode exceder o saldo; quitação depende do saldo zero.
12. `VENCIDO` é indicador calculado quando há saldo e vencimento anterior à data corrente; não substitui status de liquidação.
13. Cadastro de produto nunca altera saldo. Não existem saídas de estoque nesta versão.

## Observação de concorrência

As operações críticas são transacionais, o saldo usa `@Version`, as chaves únicas protegem reenvios e o SQLite serializa gravações. O serviço também serializa recebimentos/baixas no processo local e configura timeout de escrita. Para implantação com múltiplas instâncias ou carga elevada, recomenda-se um banco com bloqueio/concorrência multiusuário mais robustos.
