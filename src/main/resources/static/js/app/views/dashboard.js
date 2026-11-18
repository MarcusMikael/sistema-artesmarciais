/* Tela inicial: visão geral do mês. */
import { esc, icon, money } from '../../common.js';
import { capital, firstName, formatDayMonth, mensagemCobranca, mesNome, modalidade } from '../format.js';
import { ativos, pagamentosDoMes, resumo, situacao, state } from '../state.js';
import { avatar, monthPicker, pageHeader, whatsappButton } from '../ui.js';
import { revenueChart } from './reports.js';

function metric(label, value, detail, ic, tone = '') {
  return `<article class="metric ${tone}">
    <div class="metric-top"><span>${label}</span>${icon(ic, 19)}</div>
    <strong>${value}</strong>
    <small>${detail}</small>
  </article>`;
}

function waitingList(lista) {
  if (!lista.length) {
    return `<div class="empty-inline">${icon('check', 18)} Nenhum comprovante aguardando conferência.</div>`;
  }
  return `<ul class="row-list">${lista.slice(0, 6).map((p) => `
    <li class="row-item">
      ${avatar(p.aluno.nome, 'sm', p.aluno.faixaAtual)}
      <div class="row-main">
        <a href="#/alunos/${p.aluno.id}">${esc(p.aluno.nome)}</a>
        <span>${esc(modalidade(p.aluno.modalidade))} · ${money(p.valor)}</span>
      </div>
      <button class="button primary small" data-action="confirm" data-id="${p.id}">Conferir</button>
    </li>`).join('')}</ul>`;
}

function lateList(lista) {
  if (!lista.length) {
    return `<div class="empty-inline">${icon('check', 18)} Nenhuma mensalidade em atraso.</div>`;
  }
  return `<ul class="row-list">${lista.slice(0, 6).map((p) => `
    <li class="row-item">
      ${avatar(p.aluno.nome, 'sm', p.aluno.faixaAtual)}
      <div class="row-main">
        <a href="#/alunos/${p.aluno.id}">${esc(p.aluno.nome)}</a>
        <span class="text-late">Venceu ${esc(formatDayMonth(p.dataVencimento))} · ${p.diasAtraso} ${p.diasAtraso === 1 ? 'dia' : 'dias'} · ${money(p.valor)}</span>
      </div>
      ${whatsappButton(p.aluno.telefone, mensagemCobranca(p), 'Cobrar', 'button ghost small')
        || `<button class="button outline small" data-action="receipt" data-id="${p.id}">Comprovante</button>`}
    </li>`).join('')}</ul>`;
}

export function dashboardPage() {
  const pays = pagamentosDoMes();
  const r = resumo(pays);
  const nAtivos = ativos().length;
  const pct = r.previsto ? Math.round((r.recebido / r.previsto) * 100) : 0;
  const ordem = (a, b) => a.aluno.nome.localeCompare(b.aluno.nome, 'pt-BR');
  const waiting = pays.filter((p) => situacao(p) === 'waiting').sort(ordem);
  const late = pays.filter((p) => situacao(p) === 'late').sort((a, b) => b.diasAtraso - a.diasAtraso);
  const bar = (n, cls) => (r.total ? `<i class="${cls}" style="width:${(n / r.total) * 100}%"></i>` : '');
  const nome = firstName(state.user && state.user.nome);
  const ultimos = state.receita.slice(-6);

  return `<main class="page page-wide">
    ${pageHeader(nome && nome !== 'Administrador' ? `Olá, ${nome}` : 'Visão geral',
      `Resumo de ${mesNome(state.ref.mes)} de ${state.ref.ano}.`,
      `${monthPicker()}<a class="button primary" href="#/alunos/novo">${icon('plus', 18)} Novo aluno</a>`)}

    <section class="metric-grid" aria-label="Indicadores do mês">
      ${metric('Alunos ativos', nAtivos, `${state.alunos.length - nAtivos} ${state.alunos.length - nAtivos === 1 ? 'inativo' : 'inativos'}`, 'students')}
      ${metric('Receita confirmada', money(r.recebido), `${pct}% de ${money(r.previsto)} previstos`, 'check', 'tone-green')}
      ${metric('Aguardando conferência', r.nWaiting, `${money(r.aguardando)} em comprovantes`, 'clock', 'tone-yellow')}
      ${metric('Pagamentos pendentes', r.nPending,
        r.nPending ? `${money(r.aberto)} em aberto${r.nLate ? ` · <b class="text-late">${r.nLate} em atraso</b>` : ''}` : 'Nenhuma pendência',
        'warning', r.nLate ? 'tone-red' : '')}
    </section>

    <div class="dash-grid">
      <section class="panel">
        <div class="panel-heading">
          <div><h2>Prontos para conferência</h2><p>${r.nWaiting} ${r.nWaiting === 1 ? 'comprovante recebido' : 'comprovantes recebidos'}</p></div>
          <a class="text-button" href="#/financeiro" data-filter-link="waiting">Ver todos</a>
        </div>
        ${waitingList(waiting)}
      </section>

      <section class="panel">
        <div class="panel-heading">
          <div><h2>Em atraso</h2><p>Mensalidades vencidas sem comprovante</p></div>
          <a class="text-button" href="#/financeiro" data-filter-link="late">Ver todos</a>
        </div>
        ${lateList(late)}
      </section>

      <section class="panel panel-pad">
        <div class="panel-heading flush">
          <div><h2>Pagamentos do mês</h2><p>${capital(mesNome(state.ref.mes))} ${state.ref.ano} · ${r.total} mensalidades</p></div>
        </div>
        <div class="status-bar" role="img" aria-label="${r.nPaid} pagos, ${r.nWaiting} aguardando, ${r.nPending} em aberto">
          ${bar(r.nPaid, 'bar-paid')}${bar(r.nWaiting, 'bar-waiting')}${bar(r.nPending - r.nLate, 'bar-pending')}${bar(r.nLate, 'bar-late')}
        </div>
        <ul class="legend">
          <li><span><i class="dot dot-paid"></i>Pagos</span><strong>${r.nPaid}</strong></li>
          <li><span><i class="dot dot-waiting"></i>Aguardando confirmação</span><strong>${r.nWaiting}</strong></li>
          <li><span><i class="dot dot-pending"></i>Pendentes no prazo</span><strong>${r.nPending - r.nLate}</strong></li>
          <li><span><i class="dot dot-late"></i>Atrasados</span><strong>${r.nLate}</strong></li>
        </ul>
        <a class="text-link" href="#/financeiro">Abrir financeiro ${icon('arrow', 18)}</a>
      </section>

      <section class="panel panel-pad">
        <div class="panel-heading flush">
          <div><h2>Receita dos últimos meses</h2><p>Valores confirmados por mês</p></div>
          <a class="text-button" href="#/relatorios">Relatórios</a>
        </div>
        ${ultimos.length ? revenueChart(ultimos, { compact: true }) : '<div class="loading"><span class="spinner"></span></div>'}
      </section>
    </div>
  </main>`;
}
