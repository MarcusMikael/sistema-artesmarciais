/* Financeiro: mensalidades do mês, com conferência e cobrança. */
import { esc, icon, money } from '../../common.js';
import { formatDate, mesNome, modalidade, norm } from '../format.js';
import { ativos, isCurrentRef, pagamentosDoMes, resumo, situacao, state } from '../state.js';
import { avatar, emptyState, monthPicker, pageHeader, paymentActions, paymentChip, vencimentoText } from '../ui.js';

// Primeiro o que pede ação: conferir, cobrar atrasados, pendentes, pagos.
const PRIORIDADE = { waiting: 0, late: 1, pending: 2, paid: 3 };

export function filteredPayments() {
  const q = norm(state.finance.query);
  const f = state.finance.filter;
  return pagamentosDoMes()
    .filter((p) => norm(p.aluno.nome).includes(q))
    .filter((p) => {
      const s = situacao(p);
      if (f === 'all') return true;
      if (f === 'pending') return s === 'pending' || s === 'late';
      return s === f;
    })
    .sort((x, y) => PRIORIDADE[situacao(x)] - PRIORIDADE[situacao(y)]
      || (situacao(x) === 'late' ? y.diasAtraso - x.diasAtraso : 0)
      || x.aluno.nome.localeCompare(y.aluno.nome, 'pt-BR'));
}

function paymentRows(list) {
  return `<div class="table-wrap desktop-only">
    <table class="data-table">
      <thead><tr><th>Aluno</th><th>Vencimento</th><th class="num">Valor</th><th>Situação</th><th><span class="sr-only">Ações</span></th></tr></thead>
      <tbody>
        ${list.map((p) => `<tr class="state-${situacao(p)}">
          <td><a class="cell-person" href="#/alunos/${p.aluno.id}">${avatar(p.aluno.nome, 'sm', p.aluno.faixaAtual)}<span><strong>${esc(p.aluno.nome)}</strong><small>${esc(modalidade(p.aluno.modalidade))}${p.aluno.status === 'INATIVO' ? ' · Inativo' : ''}</small></span></a></td>
          <td>${situacao(p) === 'paid' ? `<small class="muted">${vencimentoText(p)}</small>` : esc(formatDate(p.dataVencimento))}</td>
          <td class="num"><strong>${money(p.valor)}</strong></td>
          <td>${paymentChip(p, { short: true })}</td>
          <td class="cell-actions">${paymentActions(p, { compact: true })}</td>
        </tr>`).join('')}
      </tbody>
    </table>
  </div>
  <div class="card-list mobile-only">
    ${list.map((p) => `<article class="payment-card state-${situacao(p)}">
      <div class="payment-top">
        ${avatar(p.aluno.nome, '', p.aluno.faixaAtual)}
        <a class="payment-name" href="#/alunos/${p.aluno.id}"><strong>${esc(p.aluno.nome)}</strong><small>${vencimentoText(p)}</small></a>
        <strong class="payment-value">${money(p.valor)}</strong>
      </div>
      ${paymentChip(p)}
      <div class="payment-actions">${paymentActions(p)}</div>
    </article>`).join('')}
  </div>`;
}

export function paymentResults() {
  const list = filteredPayments();
  if (list.length) return paymentRows(list);

  const semMensalidades = !pagamentosDoMes().length && ativos().length && !isCurrentRef();
  if (semMensalidades) {
    return emptyState('finance', 'Mensalidades ainda não geradas',
      `Gere as mensalidades de ${mesNome(state.ref.mes)} para os alunos ativos.`,
      `<button class="button primary" data-action="generate">${icon('plus', 18)} Gerar mensalidades</button>`);
  }
  return emptyState('search', 'Nenhuma mensalidade encontrada', 'Tente ajustar a busca ou o filtro.');
}

export function financePage() {
  const r = resumo(pagamentosDoMes());
  const f = state.finance.filter;
  const filters = [['all', 'Todas', r.total], ['late', 'Atrasadas', r.nLate], ['pending', 'Em aberto', r.nPending],
    ['waiting', 'Aguardando', r.nWaiting], ['paid', 'Pagas', r.nPaid]];

  return `<main class="page page-wide">
    ${pageHeader('Financeiro', `Mensalidades de ${mesNome(state.ref.mes)} de ${state.ref.ano}.`,
      `${monthPicker()}<button class="button outline" data-action="export-payments">${icon('download', 18)} <span class="hide-mobile">Exportar</span></button>`)}

    <section class="summary-strip" aria-label="Resumo do mês">
      <div><small>Previsto</small><strong>${money(r.previsto)}</strong></div>
      <div class="tone-green"><small>Recebido</small><strong>${money(r.recebido)}</strong></div>
      <div class="tone-yellow"><small>Aguardando</small><strong>${money(r.aguardando)}</strong></div>
      <div class="tone-red"><small>Em aberto${r.nLate ? ` · ${r.nLate} em atraso` : ''}</small><strong>${money(r.aberto)}</strong></div>
    </section>

    <div class="toolbar">
      <label class="input-wrap search">${icon('search', 19)}
        <input id="finance-search" type="search" aria-label="Buscar aluno no financeiro" placeholder="Buscar aluno pelo nome" value="${esc(state.finance.query)}">
      </label>
      <div class="segmented scroll" role="group" aria-label="Filtrar por situação">
        ${filters.map(([k, label, n]) => `<button class="${f === k ? 'selected' : ''}" data-action="finance-filter" data-filter="${k}" aria-pressed="${f === k}">${label} <b>${n}</b></button>`).join('')}
      </div>
    </div>

    <section class="panel flat" id="payment-results">${paymentResults()}</section>
    <p class="info-note">${icon('info', 18)} Recebeu o comprovante? Registre o recebimento e confirme depois de conferir. Alunos inativos só aparecem aqui se a mensalidade já tiver comprovante.</p>
  </main>`;
}
