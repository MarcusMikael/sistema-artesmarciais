/* Relatórios: receita por mês, alunos por modalidade e exportações. */
import { esc, icon, money } from '../../common.js';
import { MODALIDADES, capital, mesCurto, mesNome } from '../format.js';
import { ativos, state } from '../state.js';
import { pageHeader } from '../ui.js';

/** Gráfico de barras empilhadas (recebido / aguardando / em aberto) por mês. */
export function revenueChart(meses, { compact = false } = {}) {
  const max = Math.max(1, ...meses.map((m) => Number(m.previsto)));
  const pct = (v) => `${(Number(v) / max) * 100}%`;
  return `<div class="chart ${compact ? 'chart-compact' : ''}">
    <div class="chart-bars">
      ${meses.map((m) => {
        const titulo = `${capital(mesNome(m.mes))} ${m.ano}: recebido ${money(m.recebido)}, aguardando ${money(m.aguardando)}, em aberto ${money(m.pendente)}`;
        return `<div class="chart-col" title="${esc(titulo)}">
          ${compact ? '' : `<span class="chart-value">${Number(m.recebido) ? money(m.recebido).replace(/,00$/, '') : ''}</span>`}
          <div class="chart-stack" role="img" aria-label="${esc(titulo)}">
            <i class="bar-pending" style="height:${pct(m.pendente)}"></i>
            <i class="bar-waiting" style="height:${pct(m.aguardando)}"></i>
            <i class="bar-paid" style="height:${pct(m.recebido)}"></i>
          </div>
          <span class="chart-label">${mesCurto(m.mes)}${m.mes === 1 || meses[0] === m ? `<small>${String(m.ano).slice(2)}</small>` : ''}</span>
        </div>`;
      }).join('')}
    </div>
    <ul class="legend inline">
      <li><span><i class="dot dot-paid"></i>Recebido</span></li>
      <li><span><i class="dot dot-waiting"></i>Aguardando</span></li>
      <li><span><i class="dot dot-late"></i>Em aberto</span></li>
    </ul>
  </div>`;
}

function modalidadeBreakdown() {
  const lista = ativos();
  const linhas = Object.entries(MODALIDADES).map(([k, label]) => {
    const alunos = lista.filter((a) => a.modalidade === k);
    return { label, n: alunos.length, valor: alunos.reduce((s, a) => s + Number(a.valorMensalidade || 0), 0) };
  });
  const max = Math.max(1, ...linhas.map((l) => l.n));
  return `<ul class="hbars">${linhas.map((l) => `
    <li>
      <div class="hbar-top"><span>${l.label}</span><strong>${l.n} ${l.n === 1 ? 'aluno' : 'alunos'}</strong></div>
      <div class="hbar"><i style="width:${(l.n / max) * 100}%"></i></div>
      <small>${money(l.valor)} por mês</small>
    </li>`).join('')}</ul>`;
}

export function reportsPage() {
  const meses = state.receita;
  const soma = (k) => meses.reduce((s, m) => s + Number(m[k]), 0);
  const previsto = soma('previsto');
  const recebido = soma('recebido');
  const comCobranca = meses.filter((m) => m.quantidade > 0).length || 1;
  const taxa = previsto ? Math.round((recebido / previsto) * 100) : 0;
  const periodo = (n) => `<button class="${state.receitaMeses === n ? 'selected' : ''}" data-action="report-period" data-meses="${n}">${n} meses</button>`;

  return `<main class="page page-wide">
    ${pageHeader('Relatórios', 'Receita, recebimentos e distribuição dos alunos.',
      `<div class="segmented compact" role="group" aria-label="Período">${periodo(6)}${periodo(12)}</div>
       <button class="button outline" data-action="export-report">${icon('download', 18)} Exportar CSV</button>`)}

    <section class="metric-grid three" aria-label="Indicadores do período">
      <article class="metric tone-green"><div class="metric-top"><span>Recebido no período</span>${icon('check', 19)}</div>
        <strong>${money(recebido)}</strong><small>de ${money(previsto)} cobrados</small></article>
      <article class="metric"><div class="metric-top"><span>Média mensal recebida</span>${icon('chart', 19)}</div>
        <strong>${money(recebido / comCobranca)}</strong><small>nos meses com mensalidades</small></article>
      <article class="metric ${taxa < 80 ? 'tone-yellow' : 'tone-green'}"><div class="metric-top"><span>Taxa de recebimento</span>${icon('finance', 19)}</div>
        <strong>${taxa}%</strong><small>do valor cobrado foi confirmado</small></article>
    </section>

    <section class="panel panel-pad">
      <div class="panel-heading flush"><div><h2>Receita por mês</h2><p>Passe o mouse ou toque nas barras para ver os valores</p></div></div>
      ${revenueChart(meses)}
    </section>

    <div class="dash-grid">
      <section class="panel">
        <div class="panel-heading"><div><h2>Detalhe mensal</h2><p>Valores das mensalidades de cada mês</p></div></div>
        <div class="table-wrap">
          <table class="data-table">
            <thead><tr><th>Mês</th><th class="num">Cobrado</th><th class="num">Recebido</th><th class="num">Em aberto</th><th class="num">%</th></tr></thead>
            <tbody>
              ${meses.slice().reverse().map((m) => `<tr>
                <td>${capital(mesNome(m.mes))} ${m.ano}</td>
                <td class="num">${money(m.previsto)}</td>
                <td class="num">${money(m.recebido)}</td>
                <td class="num">${money(Number(m.pendente) + Number(m.aguardando))}</td>
                <td class="num">${Number(m.previsto) ? Math.round((m.recebido / m.previsto) * 100) : 0}%</td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel panel-pad">
        <div class="panel-heading flush"><div><h2>Alunos ativos por modalidade</h2><p>${ativos().length} alunos ativos</p></div></div>
        ${modalidadeBreakdown()}
        <div class="export-list">
          <h3>Exportar planilhas</h3>
          <button class="button ghost" data-action="export-students">${icon('download', 18)} Lista de alunos</button>
          <button class="button ghost" data-action="export-payments">${icon('download', 18)} Mensalidades de ${mesNome(state.ref.mes)}/${state.ref.ano}</button>
        </div>
      </section>
    </div>
  </main>`;
}
