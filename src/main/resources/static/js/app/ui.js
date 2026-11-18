/* Componentes de interface compartilhados entre as telas. */
import { brandHtml, esc, icon, money } from '../common.js';
import { beltStyle, capital, formatDate, formatDateTime, initials, mensagemCobranca, mesNome, whatsappLink } from './format.js';
import { situacao, state } from './state.js';

const NAV = [
  ['dashboard', 'dashboard', 'Início', '#/'],
  ['students', 'students', 'Alunos', '#/alunos'],
  ['finance', 'finance', 'Financeiro', '#/financeiro'],
  ['reports', 'chart', 'Relatórios', '#/relatorios'],
];

const userName = () => (state.user && state.user.nome) || 'Administrador';

function navLinks(active, cls) {
  return NAV.map(([key, ic, label, href]) => `
    <a class="${cls} ${active === key ? 'active' : ''}" href="${href}" ${active === key ? 'aria-current="page"' : ''}>
      ${icon(ic, 21)}<span>${label}</span>
    </a>`).join('');
}

/** Estrutura da página: menu lateral (computador) ou cabeçalho + barra inferior (celular). */
export function shell(active, page) {
  const nome = userName();
  return `<div class="app-shell">
    <aside class="sidebar">
      ${brandHtml()}
      <nav class="side-nav" aria-label="Navegação principal">${navLinks(active, 'side-link')}</nav>
      <div class="side-user">
        <span class="avatar" aria-hidden="true">${esc(initials(nome))}</span>
        <div><strong>${esc(nome)}</strong><small>${esc(state.user ? state.user.email : '')}</small></div>
        <button class="icon-button" data-action="logout" aria-label="Sair" title="Sair">${icon('logout', 19)}</button>
      </div>
    </aside>

    <header class="app-header">
      ${brandHtml()}
      <button class="avatar" data-action="toggle-menu" aria-label="Abrir menu da administração" aria-expanded="${state.menuOpen}">${esc(initials(nome))}</button>
      ${state.menuOpen ? `
        <button class="menu-scrim" data-action="toggle-menu" aria-label="Fechar menu"></button>
        <div class="profile-menu">
          <div><strong>${esc(nome)}</strong><small>${esc(state.user ? state.user.email : '')}</small></div>
          <button class="button ghost logout-button" data-action="logout">${icon('logout', 18)} Sair</button>
        </div>` : ''}
    </header>

    <div class="main">${page}</div>

    <nav class="bottom-nav" aria-label="Navegação principal">${navLinks(active, 'bottom-link')}</nav>
    ${modalHtml()}
  </div>`;
}

export function pageHeader(title, subtitle, actions = '') {
  return `<div class="page-header">
    <div>
      <h1>${esc(title)}</h1>
      ${subtitle ? `<p>${subtitle}</p>` : ''}
    </div>
    ${actions ? `<div class="page-actions">${actions}</div>` : ''}
  </div>`;
}

export function monthPicker() {
  return `<div class="month-picker">
    <button class="month-nav" data-action="month-prev" aria-label="Mês anterior">${icon('back', 18)}</button>
    <span class="month-label">${icon('calendar', 18)}<span>${capital(mesNome(state.ref.mes))} ${state.ref.ano}</span></span>
    <button class="month-nav" data-action="month-next" aria-label="Próximo mês">${icon('arrow', 18)}</button>
  </div>`;
}

export function avatar(nome, size = '', faixa = null) {
  const belt = beltStyle(faixa);
  return `<span class="initials ${size}" aria-hidden="true"${belt ? ` style="${belt}"` : ''}>${esc(initials(nome))}</span>`;
}

export function statusChip(status) {
  const ativo = status === 'ATIVO';
  return `<span class="chip ${ativo ? 'chip-active' : 'chip-muted'}">${ativo ? 'Ativo' : 'Inativo'}</span>`;
}

export function paymentChip(p, { short = false } = {}) {
  const s = situacao(p);
  const map = {
    paid: ['chip-paid', 'check', 'Pago'],
    waiting: ['chip-waiting', 'clock', short ? 'Aguardando' : 'Aguardando confirmação'],
    pending: ['chip-pending', 'calendar', 'Pendente'],
    late: ['chip-late', 'warning', `Atrasado · ${p.diasAtraso} ${p.diasAtraso === 1 ? 'dia' : 'dias'}`],
  };
  const [cls, ic, label] = map[s];
  return `<span class="chip ${cls}">${icon(ic, 14)}${label}</span>`;
}

export function whatsappButton(phone, text, label = 'WhatsApp', cls = 'button ghost small') {
  const link = whatsappLink(phone, text);
  if (!link) return '';
  return `<a class="${cls} whatsapp" href="${esc(link)}" target="_blank" rel="noopener">${icon('whatsapp', 17)}${label ? `<span>${label}</span>` : ''}</a>`;
}

/** Botões da próxima ação de uma mensalidade, conforme a situação. */
export function paymentActions(p, { compact = false } = {}) {
  const s = situacao(p);
  const small = compact ? 'small' : '';
  if (s === 'waiting') {
    return `<button class="button primary ${small}" data-action="confirm" data-id="${p.id}">${icon('check', 17)} Confirmar</button>
      <button class="button ghost ${small}" data-action="undo-receipt" data-id="${p.id}" title="Voltar para pendente">${icon('undo', 17)}<span class="hide-compact">Desfazer comprovante</span></button>`;
  }
  if (s === 'paid') {
    return `<button class="button ghost ${small}" data-action="undo" data-id="${p.id}">${icon('undo', 17)} Desfazer confirmação</button>`;
  }
  return `<button class="button outline ${small}" data-action="receipt" data-id="${p.id}">${icon('receipt', 17)} Registrar comprovante</button>
    ${whatsappButton(p.aluno.telefone, mensagemCobranca(p), 'Cobrar', `button ghost ${small}`)}`;
}

export function vencimentoText(p) {
  if (situacao(p) === 'paid' && p.dataHoraConfirmacao) {
    return `Pago em ${esc(formatDateTime(p.dataHoraConfirmacao))}`;
  }
  return `${p.atrasado ? 'Venceu' : 'Vence'} em ${esc(formatDate(p.dataVencimento))}`;
}

export function emptyState(ic, title, text, extra = '') {
  return `<div class="empty-state">${icon(ic, 28)}<h2>${esc(title)}</h2><p>${esc(text)}</p>${extra}</div>`;
}

export function loadingState() {
  return '<div class="loading"><span class="spinner"></span> Carregando…</div>';
}

/* ---------- Diálogos ---------- */

function modalHtml() {
  const m = state.modal;
  if (!m) return '';
  let title; let label; let danger = false; let requireCheck = false; let content;

  if (m.type === 'confirm') {
    title = 'Confirmar pagamento'; label = 'Confirmar pagamento'; requireCheck = true;
    content = `<p>Você está confirmando a mensalidade de <strong>${esc(m.pagamento.aluno.nome)}</strong>.</p>
      <div class="modal-summary"><span>${capital(mesNome(m.pagamento.mesReferencia))} de ${m.pagamento.anoReferencia}</span><strong>${money(m.pagamento.valor)}</strong></div>
      <p>Confirme somente após conferir o comprovante e os dados do pagamento.</p>`;
  } else if (m.type === 'undo') {
    title = 'Desfazer confirmação?'; label = 'Desfazer confirmação'; danger = true;
    content = `<p>O pagamento de <strong>${esc(m.pagamento.aluno.nome)}</strong> voltará para “Aguardando confirmação”.</p>`;
  } else if (m.type === 'undo-receipt') {
    title = 'Desfazer comprovante?'; label = 'Voltar para pendente'; danger = true;
    content = `<p>A mensalidade de <strong>${esc(m.pagamento.aluno.nome)}</strong> voltará para “Pendente”.</p>
      <p>Use quando o comprovante foi registrado por engano.</p>`;
  } else if (m.type === 'delete-grad') {
    title = 'Remover graduação?'; label = 'Remover'; danger = true;
    content = `<p>A graduação <strong>${esc(m.graduacao.faixa)}</strong> de ${esc(formatDate(m.graduacao.data))} será removida do histórico.</p>`;
  } else {
    title = 'Excluir aluno?'; label = 'Excluir aluno'; danger = true;
    content = `<p>O cadastro de <strong>${esc(m.aluno.nome)}</strong> será removido, junto com as graduações.</p>
      <p>Alunos com comprovante registrado ou pagamento confirmado não podem ser excluídos — nesse caso, altere o status para Inativo.</p>
      <p>Esta ação não pode ser desfeita.</p>`;
  }

  return `<div class="modal-layer" role="presentation">
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <button class="modal-close" data-action="modal-cancel" aria-label="Fechar diálogo">${icon('x')}</button>
      <span class="modal-icon ${danger ? 'danger' : ''}">${icon(danger ? 'warning' : 'check', 25)}</span>
      <h2 id="modal-title">${title}</h2>
      <div class="modal-content">${content}</div>
      ${requireCheck ? `<label class="check-row"><input type="checkbox" id="modal-check"><span>${icon('check', 15)}</span><b>Conferi o comprovante e os dados do pagamento.</b></label>` : ''}
      <div class="modal-actions">
        <button class="button ${danger ? 'danger' : 'primary'}" data-action="modal-ok" ${requireCheck ? 'disabled' : ''}>${label}</button>
        <button class="button ghost" data-action="modal-cancel">Cancelar</button>
      </div>
    </div>
  </div>`;
}

/* ---------- Avisos ---------- */

let toastTimer = null;

export function toast(message, type = 'success') {
  let el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    el.setAttribute('role', 'status');
    document.body.appendChild(el);
  }
  el.className = `toast ${type === 'error' ? 'error' : ''}`;
  el.innerHTML = `<span>${icon(type === 'error' ? 'x' : 'check', 16)}</span>${esc(message)}`;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, type === 'error' ? 4500 : 2800);
}
