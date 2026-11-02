CREATE TABLE fornecedores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    razao_social TEXT NOT NULL,
    nome_fantasia TEXT,
    cpf_cnpj TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL,
    telefone TEXT,
    endereco TEXT,
    cidade TEXT,
    uf TEXT,
    cep TEXT,
    ativo INTEGER NOT NULL DEFAULT 1,
    criado_em TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
);
CREATE TABLE produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    descricao TEXT NOT NULL,
    categoria TEXT,
    unidade_medida TEXT NOT NULL,
    preco_referencia_centavos INTEGER NOT NULL CHECK (preco_referencia_centavos >= 0),
    estoque_minimo_milesimos INTEGER NOT NULL CHECK (estoque_minimo_milesimos >= 0),
    ativo INTEGER NOT NULL DEFAULT 1,
    criado_em TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
);
CREATE TABLE pedidos_compra (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero TEXT NOT NULL UNIQUE,
    fornecedor_id INTEGER NOT NULL REFERENCES fornecedores(id),
    data_emissao TEXT NOT NULL,
    previsao_entrega TEXT,
    prazo_pagamento_dias INTEGER NOT NULL CHECK (prazo_pagamento_dias >= 0),
    valor_total_centavos INTEGER NOT NULL CHECK (valor_total_centavos >= 0),
    status TEXT NOT NULL,
    observacoes TEXT,
    criado_em TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
);
CREATE TABLE itens_pedido (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id INTEGER NOT NULL REFERENCES pedidos_compra(id),
    produto_id INTEGER NOT NULL REFERENCES produtos(id),
    quantidade_solicitada_milesimos INTEGER NOT NULL CHECK (quantidade_solicitada_milesimos > 0),
    quantidade_recebida_milesimos INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_recebida_milesimos >= 0),
    valor_unitario_centavos INTEGER NOT NULL CHECK (valor_unitario_centavos > 0),
    valor_total_centavos INTEGER NOT NULL CHECK (valor_total_centavos >= 0),
    UNIQUE(pedido_id, produto_id)
);
CREATE TABLE entradas_estoque (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero TEXT NOT NULL UNIQUE,
    pedido_id INTEGER NOT NULL REFERENCES pedidos_compra(id),
    data_entrada TEXT NOT NULL,
    valor_total_centavos INTEGER NOT NULL CHECK (valor_total_centavos > 0),
    chave_idempotencia TEXT NOT NULL UNIQUE,
    criado_em TEXT NOT NULL
);
CREATE TABLE itens_entrada (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entrada_id INTEGER NOT NULL REFERENCES entradas_estoque(id),
    item_pedido_id INTEGER NOT NULL REFERENCES itens_pedido(id),
    quantidade_recebida_milesimos INTEGER NOT NULL CHECK (quantidade_recebida_milesimos > 0),
    valor_unitario_centavos INTEGER NOT NULL CHECK (valor_unitario_centavos > 0),
    valor_total_centavos INTEGER NOT NULL CHECK (valor_total_centavos > 0),
    UNIQUE(entrada_id, item_pedido_id)
);
CREATE TABLE saldos_estoque (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL UNIQUE REFERENCES produtos(id),
    quantidade_atual_milesimos INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_atual_milesimos >= 0),
    versao INTEGER NOT NULL DEFAULT 0,
    atualizado_em TEXT NOT NULL
);
CREATE TABLE movimentacoes_estoque (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL REFERENCES produtos(id),
    entrada_id INTEGER NOT NULL REFERENCES entradas_estoque(id),
    tipo_movimentacao TEXT NOT NULL,
    quantidade_milesimos INTEGER NOT NULL CHECK (quantidade_milesimos > 0),
    saldo_anterior_milesimos INTEGER NOT NULL,
    saldo_posterior_milesimos INTEGER NOT NULL,
    valor_movimentacao_centavos INTEGER NOT NULL CHECK (valor_movimentacao_centavos > 0),
    data_movimentacao TEXT NOT NULL
);
CREATE TABLE titulos_pagar (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero TEXT NOT NULL UNIQUE,
    fornecedor_id INTEGER NOT NULL REFERENCES fornecedores(id),
    pedido_id INTEGER NOT NULL REFERENCES pedidos_compra(id),
    entrada_id INTEGER NOT NULL UNIQUE REFERENCES entradas_estoque(id),
    data_emissao TEXT NOT NULL,
    data_vencimento TEXT NOT NULL,
    valor_original_centavos INTEGER NOT NULL CHECK (valor_original_centavos > 0),
    valor_pago_centavos INTEGER NOT NULL DEFAULT 0 CHECK (valor_pago_centavos >= 0),
    saldo_aberto_centavos INTEGER NOT NULL CHECK (saldo_aberto_centavos >= 0),
    status TEXT NOT NULL,
    observacoes TEXT,
    criado_em TEXT NOT NULL,
    atualizado_em TEXT NOT NULL
);
CREATE TABLE baixas_titulo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo_id INTEGER NOT NULL REFERENCES titulos_pagar(id),
    data_pagamento TEXT NOT NULL,
    valor_pago_centavos INTEGER NOT NULL CHECK (valor_pago_centavos > 0),
    forma_pagamento TEXT NOT NULL,
    observacoes TEXT,
    chave_idempotencia TEXT NOT NULL UNIQUE,
    criado_em TEXT NOT NULL
);
