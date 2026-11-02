# Requisitos implementados

## Escopo

Aplicação web acadêmica para cadastro de fornecedores e produtos, pedido/recebimento de compras, controle de saldos e contas a pagar. O frontend é servido pelo Spring Boot e usa exclusivamente HTML, CSS e JavaScript.

## Camadas

- **Controller:** contrato HTTP, Bean Validation e códigos de resposta.
- **Service:** transições de status, validações de negócio e transações.
- **Repository:** consultas e persistência JPA.
- **Entity:** relacionamentos e dados persistidos; entidades não são retornadas diretamente na API.
- **DTO:** entrada/saída REST, incluindo valores monetários em reais e quantidades decimais.
- **Frontend:** formulários, navegação e feedback de operações, sem cálculo financeiro autoritativo.

## Requisitos funcionais

1. CRUD lógico de fornecedores e produtos, com ativação/inativação.
2. Pedido editável enquanto rascunho, confirmável e cancelável antes do recebimento.
3. Recebimento parcial e repetível até completar as quantidades do pedido.
4. Atualização de saldo e movimentação na mesma transação da entrada.
5. Geração automática de um título por entrada e vencimento calculado pelo prazo do pedido.
6. Registro idempotente de pagamentos parciais, sem exceder o saldo do título.
7. Consultas de estoque, títulos, dashboard e posição financeira.

## Requisitos não funcionais

Persistência em SQLite local, migrações SQL versionadas e aditivas, integridade referencial, valores em centavos, quantidades em milésimos, validação de entrada, mensagens de erro padronizadas e interface responsiva. Como Flyway não possui suporte SQLite disponível no Maven Central para a configuração estável usada, um executor versionado específico do projeto gerencia as migrações.
