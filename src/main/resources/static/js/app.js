import api from "./api.js";

const content = document.querySelector("#content");
const modal = document.querySelector("#modal");
const modalTitle = document.querySelector("#modal-title");
const modalContent = document.querySelector("#modal-content");
const titleNode = document.querySelector("#page-title");
let currentView = "dashboard";
let orderLines = [];
let suppliersCache = [];
let productsCache = [];
let confirmResolver = null;
let toastTimer;

const names = { dashboard: "Dashboard", fornecedores: "Fornecedores", produtos: "Produtos", pedidos: "Pedidos de compra", entradas: "Entradas de estoque", estoque: "Consulta de estoque", titulos: "Contas a pagar", relatorios: "Relatórios" };
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]));
const currency = value => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(Number(value || 0));
const date = value => value ? new Intl.DateTimeFormat("pt-BR").format(new Date(`${value.slice(0,10)}T12:00:00`)) : "—";
const localToday = () => { const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`; };
const statusClass = status => ["RECEBIDO", "QUITADO", "NORMAL"].includes(status) ? "ok" : ["CANCELADO", "SEM ESTOQUE", "VENCIDO"].includes(status) ? "bad" : ["EM_ABERTO", "PARCIALMENTE_RECEBIDO", "PARCIALMENTE_PAGO", "ESTOQUE BAIXO"].includes(status) ? "warn" : "";
const pill = status => `<span class="status ${statusClass(status)}">${escapeHtml(status?.replaceAll("_", " ") || "—")}</span>`;
const emptyRow = (span, message = "Nenhum registro encontrado.") => `<tr><td class="empty" colspan="${span}">${message}</td></tr>`;
const showToast = (message, error = false) => {
  const toast = document.querySelector("#toast");
  toast.textContent = message;
  toast.className = `toast show${error ? " error" : ""}`;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.className = "toast", 3600);
};
const openModal = (title, body) => {
  modalTitle.textContent = title;
  modalContent.innerHTML = body;
  modal.showModal();
};
const closeModal = () => { if(modal.open)modal.close(); };
const askConfirmation = message => new Promise(resolve => {
  confirmResolver = resolve;
  openModal("Confirme a operação", `<p class="callout">${escapeHtml(message)}</p><div class="modal-actions"><button type="button" class="secondary" data-close>Voltar</button><button type="button" data-approve>Confirmar</button></div>`);
});
const moneyField = (label, name, value = "", step = "0.01", required = true, extra = "") => `<div class="field"><label for="${name}">${label}</label><input id="${name}" name="${name}" type="number" step="${step}" min="0" value="${escapeHtml(value)}" ${required ? "required" : ""} ${extra}></div>`;
const field = (label, name, value = "", type = "text", required = false, extra = "") => `<div class="field"><label for="${name}">${label}</label><input id="${name}" name="${name}" type="${type}" value="${escapeHtml(value)}" ${required ? "required" : ""} ${extra}></div>`;
const actions = content => `<div class="modal-actions"><button type="button" class="secondary" data-close>Fechar</button>${content}</div>`;
const table = (headers, rows, emptyMessage) => `<div class="panel table-wrap"><table><thead><tr>${headers.map(h => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows || emptyRow(headers.length, emptyMessage)}</tbody></table></div>`;

async function render(view = location.hash.slice(1) || "dashboard") {
  currentView = names[view] ? view : "dashboard";
  titleNode.textContent = names[currentView];
  document.querySelectorAll("[data-view]").forEach(link => link.classList.toggle("active", link.dataset.view === currentView));
  content.innerHTML = '<div class="loading">Carregando dados...</div>';
  try {
    if (currentView === "dashboard") await renderDashboard();
    else if (currentView === "fornecedores") await renderSuppliers();
    else if (currentView === "produtos") await renderProducts();
    else if (currentView === "pedidos") await renderOrders();
    else if (currentView === "entradas") await renderEntries();
    else if (currentView === "estoque") await renderStock();
    else if (currentView === "titulos") await renderTitles();
    else await renderReports();
  } catch (error) {
    content.innerHTML = `<div class="callout">Não foi possível carregar os dados: ${escapeHtml(error.message)}</div><button data-refresh>Re tentar</button>`;
  }
}

async function renderDashboard() {
  const [summary, orders, entries, titles] = await Promise.all([
    api.get("/api/relatorios/resumo"), api.get("/api/pedidos"), api.get("/api/entradas"), api.get("/api/titulos")
  ]);
  const metrics = [
    ["Fornecedores ativos", summary.fornecedoresAtivos], ["Produtos ativos", summary.produtosAtivos],
    ["Pedidos em aberto", summary.pedidosEmAberto], ["Pedidos parcialmente recebidos", summary.pedidosParciais],
    ["Produtos com estoque baixo", summary.produtosEstoqueBaixo], ["Financeiro em aberto", currency(summary.totalFinanceiroAberto)],
    ["Financeiro vencido", currency(summary.totalFinanceiroVencido)], ["Títulos quitados", summary.titulosQuitados]
  ];
  const orderRows = orders.slice(0,5).map(o => `<tr><td>${escapeHtml(o.numero)}</td><td>${escapeHtml(o.fornecedor)}</td><td>${date(o.dataEmissao)}</td><td>${pill(o.status)}</td><td>${currency(o.valorTotal)}</td></tr>`).join("");
  const entryRows = entries.slice(0,5).map(e => `<tr><td>${escapeHtml(e.numero)}</td><td>${escapeHtml(e.pedidoId)}</td><td>${date(e.dataEntrada)}</td><td>${currency(e.valorTotal)}</td></tr>`).join("");
  const upcoming = titles.filter(t => t.saldoAberto > 0).slice(0,5).map(t => `<tr><td>${escapeHtml(t.numero)}</td><td>${escapeHtml(t.fornecedor)}</td><td>${date(t.dataVencimento)}</td><td>${currency(t.saldoAberto)}</td></tr>`).join("");
  content.innerHTML = `<div class="metrics">${metrics.map(([label,value]) => `<article class="metric"><div class="metric-label">${label}</div><div class="metric-value">${escapeHtml(value)}</div><div class="metric-note">Dados atuais do sistema</div></article>`).join("")}</div>
  <div class="dashboard-grid"><section class="panel"><div class="panel-head"><h2 class="section-title">Pedidos recentes</h2><a href="#pedidos">Ver todos</a></div>${table(["Pedido","Fornecedor","Emissão","Status","Total"],orderRows)}</section>
  <section class="panel"><div class="panel-head"><h2 class="section-title">Entradas recentes</h2><a href="#entradas">Ver todas</a></div>${table(["Entrada","Pedido","Data","Valor"],entryRows)}</section>
  <section class="panel"><div class="panel-head"><h2 class="section-title">Próximos títulos em aberto</h2><a href="#titulos">Ver todos</a></div>${table(["Título","Fornecedor","Vencimento","Saldo"],upcoming)}</section></div>`;
}

async function renderSuppliers(search = "") {
  const values = await api.get(`/api/fornecedores${search ? `?q=${encodeURIComponent(search)}` : ""}`);
  const rows = values.map(v => `<tr><td>${escapeHtml(v.codigo)}</td><td>${escapeHtml(v.razaoSocial)}</td><td>${escapeHtml(v.cpfCnpj)}</td><td>${escapeHtml(v.email)}</td><td>${pill(v.ativo ? "ATIVO" : "INATIVO")}</td><td><div class="actions"><button class="small secondary" data-edit-supplier="${v.id}">Editar</button><button class="small ${v.ativo ? "danger" : "secondary"}" data-toggle-supplier="${v.id}" data-active="${v.ativo}">${v.ativo ? "Inativar" : "Reativar"}</button></div></td></tr>`).join("");
  content.innerHTML = `<div class="toolbar"><div><h2>Cadastro de fornecedores</h2><p>Pesquise, cadastre e mantenha seus parceiros.</p></div><div class="toolbar-actions"><input class="search" data-search="suppliers" placeholder="Nome, código ou documento" value="${escapeHtml(search)}"><button data-new-supplier>＋ Novo fornecedor</button></div></div>${table(["Código","Razão social","CPF/CNPJ","E-mail","Situação","Ações"],rows)}`;
}

function supplierForm(value = {}) {
  openModal(value.id ? "Editar fornecedor" : "Novo fornecedor", `<form id="supplier-form" data-id="${value.id || ""}"><div class="form-grid">
    ${field("Código","codigo",value.codigo,"text",true,'maxlength="30"')}${field("Razão social / nome","razaoSocial",value.razaoSocial,"text",true,'minlength="3" maxlength="160"')}
    ${field("Nome fantasia","nomeFantasia",value.nomeFantasia)}${field("CPF/CNPJ","cpfCnpj",value.cpfCnpj,"text",true,'maxlength="18"')}
    ${field("E-mail","email",value.email,"email",true)}${field("Telefone","telefone",value.telefone)}
    ${field("Endereço","endereco",value.endereco)}${field("Cidade","cidade",value.cidade)}
    ${field("UF","uf",value.uf,"text",false,'maxlength="2"')}${field("CEP","cep",value.cep,"text",false,'maxlength="8" pattern="[0-9]{8}"')}
  </div>${actions('<button type="submit">Salvar fornecedor</button>')}</form>`);
}

async function renderProducts(search = "", category = "") {
  const params=new URLSearchParams();
  if(search)params.set("q",search);
  if(category)params.set("categoria",category);
  const values = await api.get(`/api/produtos${params.size ? `?${params}` : ""}`);
  const rows = values.map(p => `<tr><td>${escapeHtml(p.codigo)}</td><td>${escapeHtml(p.descricao)}</td><td>${escapeHtml(p.categoria || "—")}</td><td>${escapeHtml(p.unidadeMedida)}</td><td>${currency(p.precoReferencia)}</td><td>${Number(p.saldoAtual).toLocaleString("pt-BR")}</td><td>${pill(p.ativo ? "ATIVO" : "INATIVO")}</td><td><div class="actions"><button class="small secondary" data-edit-product="${p.id}">Editar</button><button class="small ${p.ativo ? "danger" : "secondary"}" data-toggle-product="${p.id}" data-active="${p.ativo}">${p.ativo ? "Inativar" : "Reativar"}</button></div></td></tr>`).join("");
  const categories=[...new Set((await api.get("/api/produtos")).map(p=>p.categoria).filter(Boolean))].sort();
  content.innerHTML = `<div class="toolbar"><div><h2>Cadastro de produtos</h2><p>O saldo é atualizado somente por entradas de estoque.</p></div><div class="toolbar-actions"><input class="search" data-search="products" placeholder="Código ou descrição" value="${escapeHtml(search)}"><select data-product-category class="search"><option value="">Todas as categorias</option>${categories.map(c=>`<option value="${escapeHtml(c)}" ${c===category?"selected":""}>${escapeHtml(c)}</option>`).join("")}</select><button data-new-product>＋ Novo produto</button></div></div>${table(["Código","Descrição","Categoria","Unidade","Preço ref.","Saldo","Situação","Ações"],rows)}`;
}

function productForm(value = {}) {
  openModal(value.id ? "Editar produto" : "Novo produto", `<form id="product-form" data-id="${value.id || ""}"><div class="form-grid">
  ${field("Código / SKU","codigo",value.codigo,"text",true)}${field("Descrição","descricao",value.descricao,"text",true)}
  ${field("Categoria","categoria",value.categoria)}<div class="field"><label for="unidadeMedida">Unidade de medida</label><select name="unidadeMedida" id="unidadeMedida" required>${["UN","KG","L","CX","M","PC"].map(u=>`<option ${value.unidadeMedida===u?"selected":""}>${u}</option>`).join("")}</select></div>
  ${moneyField("Preço de referência (R$)","precoReferencia",value.precoReferencia)}${moneyField("Estoque mínimo","estoqueMinimo",value.estoqueMinimo,"0.001")}
  </div>${actions('<button type="submit">Salvar produto</button>')}</form>`);
}

async function renderOrders(filters={}) {
  const params=new URLSearchParams();
  if(filters.fornecedorId)params.set("fornecedorId",filters.fornecedorId);
  if(filters.status)params.set("status",filters.status);
  const [orders,suppliers]=await Promise.all([api.get(`/api/pedidos${params.size?`?${params}`:""}`),api.get("/api/fornecedores")]);
  const rows = orders.map(o => `<tr><td>${escapeHtml(o.numero)}</td><td>${escapeHtml(o.fornecedor)}</td><td>${date(o.dataEmissao)}</td><td>${date(o.previsaoEntrega)}</td><td>${currency(o.valorTotal)}</td><td>${pill(o.status)}</td><td><div class="actions"><button class="small secondary" data-order-detail="${o.id}">Ver</button>${o.status==="RASCUNHO"?`<button class="small secondary" data-edit-order="${o.id}">Editar</button><button class="small" data-confirm-order="${o.id}">Confirmar</button>`:""}${["RASCUNHO","EM_ABERTO"].includes(o.status)?`<button class="small danger" data-cancel-order="${o.id}">Cancelar</button>`:""}</div></td></tr>`).join("");
  content.innerHTML = `<div class="toolbar"><div><h2>Pedidos de compra</h2><p>Crie rascunhos, confirme e acompanhe recebimentos.</p></div><div class="toolbar-actions"><select class="search" data-order-supplier><option value="">Todos os fornecedores</option>${suppliers.map(s=>`<option value="${s.id}" ${String(filters.fornecedorId||"")===String(s.id)?"selected":""}>${escapeHtml(s.codigo)} · ${escapeHtml(s.razaoSocial)}</option>`).join("")}</select><select class="search" data-order-status><option value="">Todos os status</option>${["RASCUNHO","EM_ABERTO","PARCIALMENTE_RECEBIDO","RECEBIDO","CANCELADO"].map(s=>`<option value="${s}" ${filters.status===s?"selected":""}>${s.replaceAll("_"," ")}</option>`).join("")}</select><button data-new-order>＋ Novo pedido</button></div></div>${table(["Número","Fornecedor","Emissão","Previsão","Total","Status","Ações"],rows)}`;
}

async function newOrderForm(existing = null) {
  const [suppliers, products] = await Promise.all([api.get("/api/fornecedores"), api.get("/api/produtos")]);
  suppliersCache = suppliers.filter(s => s.ativo);
  productsCache = products.filter(p => p.ativo);
  orderLines = existing ? existing.itens.map(item=>({productId:item.produtoId,quantity:item.quantidadeSolicitada,price:item.valorUnitario})) : [{}];
  if (!suppliersCache.length || !productsCache.length) { showToast("Cadastre primeiro um fornecedor e um produto ativos.", true); return; }
  openModal(existing ? `Editar pedido ${existing.numero}` : "Novo pedido de compra", `<form id="order-form" data-id="${existing?.id||""}"><div class="form-grid">
    <div class="field"><label for="fornecedorId">Fornecedor</label><select id="fornecedorId" name="fornecedorId" required>${suppliersCache.map(s=>`<option value="${s.id}" ${String(existing?.fornecedorId||"")===String(s.id)?"selected":""}>${escapeHtml(s.codigo)} · ${escapeHtml(s.razaoSocial)}</option>`).join("")}</select></div>
    ${field("Previsão de entrega","previsaoEntrega",existing?.previsaoEntrega||"","date")}
    ${field("Prazo de pagamento (dias)","prazoPagamentoDias",existing?.prazoPagamentoDias??"30","number",true,'min="0"')}
    ${field("Observações","observacoes",existing?.observacoes||"")}
  </div><div class="panel-head" style="padding-left:0;padding-right:0;margin-top:18px"><h3 class="section-title">Itens do pedido</h3><button type="button" class="small secondary" data-add-order-line>＋ Adicionar item</button></div><div id="order-lines"></div><div class="summary"><span>Total do pedido</span><strong id="order-total">R$ 0,00</strong></div>${actions('<button type="submit">Salvar rascunho</button>')}</form>`);
  drawOrderLines();
}

function drawOrderLines() {
  const host = document.querySelector("#order-lines");
  if (!host) return;
  host.innerHTML = orderLines.map((line,index)=>`<div class="line-item" data-line="${index}">
    <div class="field"><label>Produto</label><select data-product required><option value="">Selecione</option>${productsCache.map(p=>`<option value="${p.id}" ${String(line.productId||"")===String(p.id)?"selected":""}>${escapeHtml(p.codigo)} · ${escapeHtml(p.descricao)}</option>`).join("")}</select></div>
    <div class="field"><label>Quantidade</label><input data-quantity type="number" min="0.001" step="0.001" value="${escapeHtml(line.quantity||"1")}" required></div>
    <div class="field"><label>Preço unitário</label><input data-price type="number" min="0.01" step="0.01" value="${escapeHtml(line.price||"")}" required></div>
    <div class="field"><label>Subtotal</label><input data-subtotal readonly value="${currency((Number(line.quantity||0)*Number(line.price||0)))}"></div>
    <button type="button" class="small danger" data-remove-line="${index}" aria-label="Remover item">×</button>
  </div>`).join("");
  updateOrderTotal();
}

function updateOrderTotal() {
  let total=0;
  document.querySelectorAll(".line-item").forEach(row=>{
    const amount=Number(row.querySelector("[data-quantity]").value||0)*Number(row.querySelector("[data-price]").value||0);
    row.querySelector("[data-subtotal]").value=currency(amount);
    total+=amount;
  });
  const node=document.querySelector("#order-total"); if(node)node.textContent=currency(total);
}

async function renderEntries() {
  const [entries, orders] = await Promise.all([api.get("/api/entradas"), api.get("/api/pedidos")]);
  const openOrders=orders.filter(o=>["EM_ABERTO","PARCIALMENTE_RECEBIDO"].includes(o.status));
  const rows=entries.map(e=>`<tr><td>${escapeHtml(e.numero)}</td><td>${escapeHtml(e.pedidoId)}</td><td>${date(e.dataEntrada)}</td><td>${currency(e.valorTotal)}</td><td>${escapeHtml(e.numeroTitulo||"—")}</td></tr>`).join("");
  content.innerHTML=`<div class="toolbar"><div><h2>Recebimento de mercadorias</h2><p>Uma entrada atualiza o estoque e gera o título automaticamente.</p></div></div>
  <div class="panel" style="padding:18px;margin-bottom:18px"><form id="entry-form"><div class="form-grid"><div class="field full"><label for="entradaPedido">Pedido confirmado</label><select id="entradaPedido" name="pedidoId" required><option value="">Selecione um pedido</option>${openOrders.map(o=>`<option value="${o.id}">${escapeHtml(o.numero)} · ${escapeHtml(o.fornecedor)} · ${escapeHtml(o.status)}</option>`).join("")}</select></div>${field("Data de entrada","dataEntrada",localToday(),"date",true)}</div><div id="pending-items" class="callout" style="margin-top:14px">Selecione um pedido para carregar os itens pendentes.</div><div class="summary"><span>Valor desta entrada</span><strong id="entry-total">R$ 0,00</strong></div><button id="submit-entry" type="submit" disabled>Confirmar entrada</button></form></div>
  <h2 class="section-title" style="margin:22px 0 12px">Histórico de entradas</h2>${table(["Entrada","Pedido","Data","Valor recebido","Título gerado"],rows)}`;
  if (!openOrders.length) document.querySelector("#pending-items").textContent="Não há pedidos confirmados com quantidades pendentes.";
}

async function loadPending(orderId) {
  const host=document.querySelector("#pending-items");
  if (!orderId) { host.className="callout";host.textContent="Selecione um pedido para carregar os itens pendentes.";document.querySelector("#submit-entry").disabled=true;return; }
  const items=await api.get(`/api/pedidos/${orderId}/pendencias`);
  if (!items.length) { host.className="callout";host.textContent="Este pedido não possui quantidades pendentes.";document.querySelector("#submit-entry").disabled=true;return; }
  host.className="table-wrap";
  host.innerHTML=`<table><thead><tr><th>Produto</th><th>Solicitada</th><th>Recebida</th><th>Pendente</th><th>Receber agora</th></tr></thead><tbody>${items.map(i=>`<tr data-receive-line="${i.id}" data-price="${i.valorUnitario}"><td>${escapeHtml(i.codigoProduto)} · ${escapeHtml(i.descricaoProduto)}</td><td>${i.quantidadeSolicitada}</td><td>${i.quantidadeRecebida}</td><td>${i.quantidadePendente}</td><td><input data-receive-quantity type="number" min="0" max="${i.quantidadePendente}" step="0.001" value="0" style="width:110px"></td></tr>`).join("")}</tbody></table>`;
  document.querySelector("#submit-entry").disabled=false;
}

async function renderStock(search="") {
  const rows=await api.get(`/api/estoque${search?`?q=${encodeURIComponent(search)}`:""}`);
  const body=rows.map(s=>`<tr><td>${escapeHtml(s.codigo)}</td><td>${escapeHtml(s.descricao)}</td><td>${escapeHtml(s.categoria||"—")}</td><td>${escapeHtml(s.unidadeMedida)}</td><td>${Number(s.saldoFisico).toLocaleString("pt-BR")}</td><td>${Number(s.estoqueMinimo).toLocaleString("pt-BR")}</td><td>${pill(s.situacao)}</td><td>${s.ultimaMovimentacao?new Intl.DateTimeFormat("pt-BR",{dateStyle:"short",timeStyle:"short"}).format(new Date(s.ultimaMovimentacao)):"—"}</td><td><button class="small secondary" data-movement="${s.produtoId}">Movimentações</button></td></tr>`).join("");
  content.innerHTML=`<div class="toolbar"><div><h2>Posição do estoque</h2><p>Saldo físico reconciliado a partir das movimentações.</p></div><input class="search" data-search="stock" placeholder="Código ou descrição" value="${escapeHtml(search)}"></div>${table(["Código","Produto","Categoria","Unidade","Saldo físico","Mínimo","Situação","Última movimentação","Histórico"],body)}`;
}

async function renderTitles(search="") {
  const status=document.querySelector("[data-title-status]")?.value||"";
  const dueOnly=document.querySelector("[data-title-overdue]")?.checked||false;
  const from=document.querySelector("[data-title-from]")?.value||"";
  const to=document.querySelector("[data-title-to]")?.value||"";
  const params=new URLSearchParams();
  if(status)params.set("status",status);
  if(dueOnly)params.set("vencidos","true");
  if(from)params.set("dataInicio",from);
  if(to)params.set("dataFim",to);
  const all=await api.get(`/api/titulos${params.size?`?${params}`:""}`);
  const term=search.trim().toLocaleLowerCase("pt-BR");
  const values=all.filter(t=>!term||t.numero.toLocaleLowerCase("pt-BR").includes(term)||t.fornecedor.toLocaleLowerCase("pt-BR").includes(term));
  const rows=values.map(t=>`<tr><td>${escapeHtml(t.numero)}</td><td>${escapeHtml(t.fornecedor)}</td><td>${escapeHtml(t.pedido)}</td><td>${date(t.dataVencimento)}</td><td>${currency(t.valorOriginal)}</td><td>${currency(t.valorPago)}</td><td>${currency(t.saldoAberto)}</td><td>${pill(t.vencido?"VENCIDO":t.status)}</td><td><div class="actions"><button class="small secondary" data-pay-history="${t.id}">Histórico</button>${t.saldoAberto>0?`<button class="small" data-pay-title="${t.id}">Registrar pagamento</button>`:""}</div></td></tr>`).join("");
  content.innerHTML=`<div class="toolbar"><div><h2>Contas a pagar</h2><p>Títulos gerados no recebimento; pagamentos preservam o histórico.</p></div><div class="toolbar-actions"><input class="search" data-search="titles" placeholder="Fornecedor ou número" value="${escapeHtml(search)}"><select class="search" data-title-status><option value="">Todos os status</option>${["EM_ABERTO","PARCIALMENTE_PAGO","QUITADO"].map(s=>`<option value="${s}" ${status===s?"selected":""}>${s.replaceAll("_"," ")}</option>`).join("")}</select><input class="search" data-title-from type="date" aria-label="Vencimento inicial" value="${from}"><input class="search" data-title-to type="date" aria-label="Vencimento final" value="${to}"><label class="muted"><input data-title-overdue type="checkbox" ${dueOnly?"checked":""}> Vencidos</label></div></div>${table(["Título","Fornecedor","Pedido","Vencimento","Original","Pago","Em aberto","Status","Ações"],rows)}`;
}

async function renderReports(filters={}) {
  const params=new URLSearchParams();
  if(filters.dataInicio)params.set("dataInicio",filters.dataInicio);
  if(filters.dataFim)params.set("dataFim",filters.dataFim);
  if(filters.fornecedorId)params.set("fornecedorId",filters.fornecedorId);
  if(filters.status)params.set("status",filters.status);
  if(filters.vencidos)params.set("vencidos","true");
  if(filters.somenteEmAberto)params.set("somenteEmAberto","true");
  const [stock, finance, suppliers]=await Promise.all([api.get("/api/relatorios/estoque"),api.get(`/api/relatorios/contas-pagar${params.size?`?${params}`:""}`),api.get("/api/fornecedores")]);
  const stockRows=stock.map(s=>`<tr><td>${escapeHtml(s.codigo)}</td><td>${escapeHtml(s.descricao)}</td><td>${escapeHtml(s.categoria||"—")}</td><td>${escapeHtml(s.unidadeMedida)}</td><td>${Number(s.saldoFisico).toLocaleString("pt-BR")}</td><td>${Number(s.estoqueMinimo).toLocaleString("pt-BR")}</td><td>${pill(s.situacao)}</td></tr>`).join("");
  const titleRows=finance.titulos.map(t=>`<tr><td>${escapeHtml(t.numero)}</td><td>${escapeHtml(t.fornecedor)}</td><td>${escapeHtml(t.pedido)}</td><td>${date(t.dataEmissao)}</td><td>${date(t.dataVencimento)}</td><td>${currency(t.valorOriginal)}</td><td>${currency(t.valorPago)}</td><td>${currency(t.saldoAberto)}</td><td>${pill(t.vencido?"VENCIDO":t.status)}</td></tr>`).join("");
  content.innerHTML=`<div class="toolbar"><div><h2>Relatórios</h2><p>Posição operacional e financeira atual.</p></div></div><form id="report-filter" class="panel" style="padding:14px;margin-bottom:16px"><div class="filters">${field("Vencimento inicial","dataInicio",filters.dataInicio||"","date")}${field("Vencimento final","dataFim",filters.dataFim||"","date")}<div class="field"><label>Fornecedor</label><select name="fornecedorId"><option value="">Todos</option>${suppliers.map(s=>`<option value="${s.id}" ${String(filters.fornecedorId||"")===String(s.id)?"selected":""}>${escapeHtml(s.codigo)} · ${escapeHtml(s.razaoSocial)}</option>`).join("")}</select></div><div class="field"><label>Status</label><select name="status"><option value="">Todos</option>${["EM_ABERTO","PARCIALMENTE_PAGO","QUITADO"].map(s=>`<option value="${s}" ${filters.status===s?"selected":""}>${s.replaceAll("_"," ")}</option>`).join("")}</select></div><label class="muted"><input name="vencidos" type="checkbox" ${filters.vencidos?"checked":""}> Somente vencidos</label><label class="muted"><input name="somenteEmAberto" type="checkbox" ${filters.somenteEmAberto?"checked":""}> Com saldo</label><button type="submit">Filtrar</button></div></form><div class="metrics"><article class="metric"><div class="metric-label">Total em aberto</div><div class="metric-value">${currency(finance.totalEmAberto)}</div></article><article class="metric"><div class="metric-label">Total vencido</div><div class="metric-value">${currency(finance.totalVencido)}</div></article><article class="metric"><div class="metric-label">Total pago</div><div class="metric-value">${currency(finance.totalPago)}</div></article></div><h2 class="section-title" style="margin:20px 0 12px">Posição de estoque</h2>${table(["Código","Produto","Categoria","Unidade","Quantidade","Mínimo","Situação"],stockRows)}<h2 class="section-title" style="margin:24px 0 12px">Contas a pagar por vencimento</h2>${table(["Título","Fornecedor","Pedido","Emissão","Vencimento","Original","Pago","Saldo","Status"],titleRows)}`;
}

function formObject(form) { return Object.fromEntries(new FormData(form).entries()); }
const digits = value => String(value || "").replace(/\D/g, "");
function validateDocument(value) {
  const s=digits(value);
  const calc=(source,weights)=>{let sum=0;source.split("").forEach((n,i)=>sum+=Number(n)*weights[i]);const mod=sum%11;return mod<2?0:11-mod;};
  if(s.length===11){if(/^(\d)\1{10}$/.test(s))return false;return calc(s.slice(0,9),[10,9,8,7,6,5,4,3,2])===Number(s[9])&&calc(s.slice(0,10),[11,10,9,8,7,6,5,4,3,2])===Number(s[10]);}
  if(s.length===14){if(/^(\d)\1{13}$/.test(s))return false;return calc(s.slice(0,12),[5,4,3,2,9,8,7,6,5,4,3,2])===Number(s[12])&&calc(s.slice(0,13),[6,5,4,3,2,9,8,7,6,5,4,3,2])===Number(s[13]);}
  return false;
}

document.addEventListener("click", async event => {
  const target=event.target.closest("button,a");
  if(!target)return;
  try {
    if(target.matches("[data-close]")) {
      const resolve=confirmResolver;confirmResolver=null;closeModal();if(resolve)resolve(false);return;
    }
    if(target.matches("[data-approve]")) {
      const resolve=confirmResolver;confirmResolver=null;closeModal();if(resolve)resolve(true);return;
    }
    if(target.matches("[data-refresh]")) { await render(currentView); return; }
    if(target.matches("[data-new-supplier]")) { supplierForm(); return; }
    if(target.matches("[data-new-product]")) { productForm(); return; }
    if(target.matches("[data-new-order]")) { await newOrderForm(); return; }
    if(target.matches("[data-edit-order]")) { await newOrderForm(await api.get(`/api/pedidos/${target.dataset.editOrder}`));return; }
    if(target.matches("[data-add-order-line]")) { orderLines.push({});drawOrderLines();return; }
    if(target.matches("[data-remove-line]")) { orderLines.splice(Number(target.dataset.removeLine),1);if(!orderLines.length)orderLines.push({});drawOrderLines();return; }
    if(target.matches("[data-edit-supplier]")) { const v=await api.get(`/api/fornecedores/${target.dataset.editSupplier}`);supplierForm(v);return; }
    if(target.matches("[data-edit-product]")) { const v=await api.get(`/api/produtos/${target.dataset.editProduct}`);productForm(v);return; }
    if(target.matches("[data-toggle-supplier]")) { const action=target.dataset.active==="true"?"inativar":"reativar";await api.patch(`/api/fornecedores/${target.dataset.toggleSupplier}/${action}`);showToast("Fornecedor atualizado.");await render();return; }
    if(target.matches("[data-toggle-product]")) { const action=target.dataset.active==="true"?"inativar":"reativar";await api.patch(`/api/produtos/${target.dataset.toggleProduct}/${action}`);showToast("Produto atualizado.");await render();return; }
    if(target.matches("[data-confirm-order]")) { const result=await api.post(`/api/pedidos/${target.dataset.confirmOrder}/confirmar`,{});showToast(`Pedido ${result.numero} confirmado.`);await render();return; }
    if(target.matches("[data-cancel-order]")) { await api.post(`/api/pedidos/${target.dataset.cancelOrder}/cancelar`,{});showToast("Pedido cancelado.");await render();return; }
    if(target.matches("[data-order-detail]")) { const o=await api.get(`/api/pedidos/${target.dataset.orderDetail}`);openModal(`Pedido ${o.numero}`,`<p><b>Fornecedor:</b> ${escapeHtml(o.fornecedor)} · ${pill(o.status)}</p>${table(["Produto","Solicitada","Recebida","Pendente","Unitário","Total"],o.itens.map(i=>`<tr><td>${escapeHtml(i.codigoProduto)} · ${escapeHtml(i.descricaoProduto)}</td><td>${i.quantidadeSolicitada}</td><td>${i.quantidadeRecebida}</td><td>${i.quantidadePendente}</td><td>${currency(i.valorUnitario)}</td><td>${currency(i.valorTotal)}</td></tr>`).join(""))}<div class="summary"><span>Total</span><strong>${currency(o.valorTotal)}</strong></div>`);return; }
    if(target.matches("[data-pay-title]")) { const t=await api.get(`/api/titulos/${target.dataset.payTitle}`);openModal("Registrar pagamento",`<div class="callout">Título ${escapeHtml(t.numero)} · ${escapeHtml(t.fornecedor)}<br>Original: ${currency(t.valorOriginal)} · Pago: ${currency(t.valorPago)} · Saldo: <b>${currency(t.saldoAberto)}</b><br>Vencimento: ${date(t.dataVencimento)}</div><form id="payment-form" data-id="${t.id}"><div class="form-grid">${field("Data do pagamento","dataPagamento",localToday(),"date",true)}${moneyField("Valor pago","valorPago","","0.01",true,`max="${t.saldoAberto}"`)}<div class="field"><label>Forma de pagamento</label><select name="formaPagamento" required>${["PIX","TRANSFERENCIA","BOLETO","DINHEIRO","OUTRO"].map(p=>`<option value="${p}">${p.replaceAll("_"," ")}</option>`).join("")}</select></div>${field("Observação","observacoes")}</div>${actions('<button type="submit">Registrar pagamento</button>')}</form>`);return; }
    if(target.matches("[data-pay-history]")) { const list=await api.get(`/api/titulos/${target.dataset.payHistory}/baixas`);openModal("Histórico de pagamentos",table(["Data","Valor","Forma","Observação"],list.map(b=>`<tr><td>${date(b.dataPagamento)}</td><td>${currency(b.valorPago)}</td><td>${escapeHtml(b.formaPagamento)}</td><td>${escapeHtml(b.observacoes||"—")}</td></tr>`).join("")));return; }
    if(target.matches("[data-movement]")) { const list=await api.get(`/api/estoque/${target.dataset.movement}/movimentacoes`);openModal("Histórico de movimentações",table(["Data","Tipo","Entrada","Quantidade","Saldo anterior","Saldo atual","Valor"],list.map(m=>`<tr><td>${new Intl.DateTimeFormat("pt-BR",{dateStyle:"short",timeStyle:"short"}).format(new Date(m.data))}</td><td>${escapeHtml(m.tipo)}</td><td>${escapeHtml(m.numeroEntrada)}</td><td>${m.quantidade}</td><td>${m.saldoAnterior}</td><td>${m.saldoPosterior}</td><td>${currency(m.valor)}</td></tr>`).join("")));return; }
  } catch(error) { showToast(error.message,true); }
});

document.addEventListener("input", event => {
  if(event.target.matches("[data-quantity],[data-price]")) updateOrderTotal();
  if(event.target.matches("[data-search]")) {
    const kind=event.target.dataset.search, value=event.target.value;
    clearTimeout(event.target.searchTimer);
    event.target.searchTimer=setTimeout(()=> {
      if(kind==="suppliers")renderSuppliers(value).catch(e=>showToast(e.message,true));
      if(kind==="products")renderProducts(value,document.querySelector("[data-product-category]")?.value||"").catch(e=>showToast(e.message,true));
      if(kind==="stock")renderStock(value).catch(e=>showToast(e.message,true));
      if(kind==="titles")renderTitles(value).catch(e=>showToast(e.message,true));
    },250);
  }
  if(event.target.matches("[data-receive-quantity]")) {
    let total=0;
    document.querySelectorAll("[data-receive-line]").forEach(row=>total+=Number(row.dataset.price)*Number(row.querySelector("[data-receive-quantity]").value||0));
    const node=document.querySelector("#entry-total");if(node)node.textContent=currency(total);
  }
});

document.addEventListener("change", async event => {
  if(event.target.matches("[data-product]")) {
    const row=event.target.closest(".line-item"), product=productsCache.find(p=>String(p.id)===event.target.value);
    if(product)row.querySelector("[data-price]").value=product.precoReferencia;
    updateOrderTotal();
  }
  if(event.target.matches("[data-product-category]")) {
    const search=document.querySelector('[data-search="products"]')?.value||"";
    renderProducts(search,event.target.value).catch(error=>showToast(error.message,true));
  }
  if(event.target.matches("[data-title-status],[data-title-overdue],[data-title-from],[data-title-to]")) {
    renderTitles(document.querySelector('[data-search="titles"]')?.value||"").catch(error=>showToast(error.message,true));
  }
  if(event.target.matches("[data-order-supplier],[data-order-status]")) {
    renderOrders({fornecedorId:document.querySelector("[data-order-supplier]").value,status:document.querySelector("[data-order-status]").value})
      .catch(error=>showToast(error.message,true));
  }
  if(event.target.matches("#entradaPedido")) {
    try { await loadPending(event.target.value); } catch(error) { showToast(error.message,true); }
  }
});

document.addEventListener("submit", async event => {
  const form=event.target;
  if(form.matches("#report-filter")) {
    event.preventDefault();
    const values=formObject(form);
    values.vencidos=form.elements.vencidos.checked;
    values.somenteEmAberto=form.elements.somenteEmAberto.checked;
    renderReports(values).catch(error=>showToast(error.message,true));
    return;
  }
  if(!form.matches("#supplier-form,#product-form,#order-form,#entry-form,#payment-form"))return;
  event.preventDefault();
  const submit=form.querySelector('[type="submit"]');
  if(submit)submit.disabled=true;
  try {
    if(form.matches("#supplier-form")) {
      const value=formObject(form);
      if(!validateDocument(value.cpfCnpj))throw new Error("Informe um CPF ou CNPJ válido.");
      value.cpfCnpj=digits(value.cpfCnpj);
      if(form.dataset.id)await api.put(`/api/fornecedores/${form.dataset.id}`,value);else await api.post("/api/fornecedores",value);
      showToast("Fornecedor salvo com sucesso.");
    } else if(form.matches("#product-form")) {
      const value=formObject(form);
      if(form.dataset.id)await api.put(`/api/produtos/${form.dataset.id}`,value);else await api.post("/api/produtos",value);
      showToast("Produto salvo com sucesso.");
    } else if(form.matches("#order-form")) {
      const lines=[...document.querySelectorAll(".line-item")].map(row=>({produtoId:Number(row.querySelector("[data-product]").value),quantidade:row.querySelector("[data-quantity]").value,valorUnitario:row.querySelector("[data-price]").value}));
      if(lines.some(i=>!i.produtoId||Number(i.quantidade)<=0||Number(i.valorUnitario)<=0))throw new Error("Informe produto, quantidade e valor válidos para cada item.");
      const value=formObject(form);value.fornecedorId=Number(value.fornecedorId);value.prazoPagamentoDias=Number(value.prazoPagamentoDias);value.itens=lines;
      const result=form.dataset.id?await api.put(`/api/pedidos/${form.dataset.id}`,value):await api.post("/api/pedidos",value);
      showToast(`Rascunho ${result.numero} salvo.`);
    } else if(form.matches("#entry-form")) {
      const lines=[...document.querySelectorAll("[data-receive-line]")].map(row=>({itemPedidoId:Number(row.dataset.receiveLine),quantidadeRecebida:row.querySelector("[data-receive-quantity]").value})).filter(i=>Number(i.quantidadeRecebida)>0);
      if(!lines.length)throw new Error("Informe ao menos uma quantidade para receber.");
      if(!(await askConfirmation("Confirmar entrada de mercadorias? Esta operação atualizará o estoque e gerará automaticamente um título a pagar.")))return;
      const value=formObject(form);value.pedidoId=Number(value.pedidoId);value.itens=lines;form.dataset.idempotencyKey ||= crypto.randomUUID();value.chaveIdempotencia=form.dataset.idempotencyKey;
      const result=await api.post("/api/entradas",value);
      const received=result.itens.map(i=>`${i.descricaoProduto}: ${i.quantidadeRecebida}`).join(", ");
      showToast(`Entrada ${result.numero} · ${currency(result.valorTotal)} · título ${result.numeroTitulo} · ${received}`);
    } else if(form.matches("#payment-form")) {
      const value=formObject(form);form.dataset.idempotencyKey ||= crypto.randomUUID();value.chaveIdempotencia=form.dataset.idempotencyKey;
      await api.post(`/api/titulos/${form.dataset.id}/baixas`,value);showToast("Pagamento registrado.");
    }
    closeModal();
    await render();
  } catch(error) { showToast(error.message,true); }
  finally { if(submit)submit.disabled=false; }
});

document.querySelector("[data-close]").addEventListener("click",closeModal);
modal.addEventListener("click",event=>{if(event.target===modal){const resolve=confirmResolver;confirmResolver=null;closeModal();if(resolve)resolve(false);}});
modal.addEventListener("cancel",()=>{const resolve=confirmResolver;confirmResolver=null;if(resolve)resolve(false);});
document.querySelector(".menu-toggle").addEventListener("click",()=>document.querySelector(".sidebar").classList.toggle("open"));
document.querySelectorAll("[data-view]").forEach(link=>link.addEventListener("click",()=>document.querySelector(".sidebar").classList.remove("open")));
window.addEventListener("hashchange",()=>render());
render();
