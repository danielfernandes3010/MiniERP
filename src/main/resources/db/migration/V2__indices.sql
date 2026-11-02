CREATE INDEX idx_pedidos_fornecedor_status ON pedidos_compra(fornecedor_id, status);
CREATE INDEX idx_itens_pedido_produto ON itens_pedido(produto_id);
CREATE INDEX idx_entradas_pedido_data ON entradas_estoque(pedido_id, data_entrada);
CREATE INDEX idx_movimentacoes_produto_data ON movimentacoes_estoque(produto_id, data_movimentacao);
CREATE INDEX idx_titulos_vencimento_status ON titulos_pagar(data_vencimento, status);
CREATE INDEX idx_titulos_fornecedor ON titulos_pagar(fornecedor_id);
CREATE INDEX idx_baixas_titulo_data ON baixas_titulo(titulo_id, data_pagamento);
