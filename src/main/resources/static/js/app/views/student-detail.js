/* Ficha do aluno: dados, contato, histórico de mensalidades e graduações. */
import { esc, icon, money } from '../../common.js';
import { FAIXAS, capital, corFaixa, formatDate, mesNome, modalidade, tempoDesde, todayIso } from '../format.js';
import { findAluno, situacao, state } from '../state.js';
import { avatar, emptyState, loadingState, paymentActions, paymentChip, statusChip, vencimentoText, whatsappButton } from '../ui.js';
import { faixaBadge } from './students.js';

function info(label, value) {
  return `<div><dt>${label}</dt><dd>${value}</dd></div>`;
}

function historico(pagamentos) {
  if (!pagamentos.length) {
    return `<div class="empty-inline">${icon('info', 18)} Nenhuma mensalidade gerada para este aluno ainda.</div>`;
  }
  return `<ul class="history">${pagamentos.map((p) => `
    <li class="history-row state-${situacao(p)}">
      <div class="history-main">
        <strong>${capital(mesNome(p.mesReferencia))} ${p.anoReferencia}</strong>
        <small>${vencimentoText(p)}</small>
      </div>
      <strong class="history-value">${money(p.valor)}</strong>
      <div class="history-status">${paymentChip(p)}</div>
      <div class="history-actions">${paymentActions(p, { compact: true })}</div>
    </li>`).join('')}</ul>`;
}

function graduacoes(lista) {
  return `${lista.length ? `<ol class="timeline">${lista.map((g) => `
      <li>
        <i class="timeline-dot" style="background:${corFaixa(g.faixa) || 'var(--muted)'}"></i>
        <div>
          <strong>${esc(g.faixa)}</strong>
          <small>${esc(formatDate(g.data))}${g.observacao ? ` · ${esc(g.observacao)}` : ''}</small>
        </div>
        <button class="icon-button" data-action="delete-grad" data-id="${g.id}" aria-label="Remover graduação ${esc(g.faixa)}" title="Remover">${icon('trash', 17)}</button>
      </li>`).join('')}</ol>`
    : `<div class="empty-inline">${icon('belt', 18)} Nenhuma graduação registrada.</div>`}
    <form class="grad-form" id="grad-form" novalidate>
      <h3>Registrar graduação</h3>
      <div class="grad-fields">
        <label class="field" data-field="faixa">
          <span class="field-label">Faixa</span>
          <span class="input-wrap"><input name="faixa" list="faixas" maxlength="50" placeholder="Ex.: Azul" autocomplete="off"></span>
          <span class="error-text" hidden></span>
        </label>
        <label class="field" data-field="data">
          <span class="field-label">Data</span>
          <span class="input-wrap"><input name="data" type="date" value="${todayIso()}" max="${todayIso()}"></span>
          <span class="error-text" hidden></span>
        </label>
      </div>
      <label class="field" data-field="observacao">
        <span class="field-label">Observação <small>(opcional)</small></span>
        <span class="input-wrap"><input name="observacao" maxlength="255" placeholder="Ex.: Exame de faixa do semestre"></span>
        <span class="error-text" hidden></span>
      </label>
      <datalist id="faixas">${FAIXAS.map((f) => `<option value="${f}">`).join('')}</datalist>
      <button class="button outline" type="submit">${icon('plus', 18)} Registrar graduação</button>
    </form>`;
}

export function studentDetailPage(id) {
  const a = findAluno(id);
  if (!a) {
    return `<main class="page">
      <a class="back-link" href="#/alunos">${icon('back', 18)} Alunos</a>
      ${emptyState('search', 'Aluno não encontrado', 'O cadastro pode ter sido excluído.')}
    </main>`;
  }
  const d = state.detalhe && state.detalhe.id === a.id ? state.detalhe : null;
  const pagos = d ? d.pagamentos.filter((p) => p.statusPagamento === 'PAGO') : [];
  const abertos = d ? d.pagamentos.filter((p) => p.statusPagamento !== 'PAGO') : [];
  const atrasados = abertos.filter((p) => p.atrasado);

  return `<main class="page page-wide">
    <a class="back-link" href="#/alunos">${icon('back', 18)} Alunos</a>

    <section class="profile">
      ${avatar(a.nome, 'lg')}
      <div class="profile-main">
        <div class="profile-title"><h1>${esc(a.nome)}</h1>${statusChip(a.status)}</div>
        <p>${esc(modalidade(a.modalidade))}${a.turmaHorario ? ` · ${esc(a.turmaHorario)}` : ''}</p>
      </div>
      <div class="profile-actions">
        ${whatsappButton(a.telefone, `Olá, ${a.nome.split(' ')[0]}!`, 'WhatsApp', 'button outline')}
        <a class="button primary" href="#/alunos/${a.id}/editar">${icon('edit', 18)} Editar</a>
      </div>
    </section>

    <div class="detail-grid">
      <div class="detail-side">
        <section class="panel panel-pad">
          <h2>Dados</h2>
          <dl class="info-grid">
            ${info('Telefone', esc(a.telefone || 'Não informado'))}
            ${info('Modalidade', esc(modalidade(a.modalidade)))}
            ${info('Turma / horário', esc(a.turmaHorario || '—'))}
            ${info('Faixa atual', faixaBadge(a.faixaAtual))}
            ${info('Mensalidade', money(a.valorMensalidade))}
            ${info('Vencimento', `Todo dia ${a.diaVencimento}`)}
            ${info('Matrícula', a.dataMatricula ? `${esc(formatDate(a.dataMatricula))}<small>${esc(tempoDesde(a.dataMatricula))}</small>` : '—')}
          </dl>
        </section>

        <section class="panel panel-pad">
          <h2>Graduações</h2>
          ${d ? graduacoes(d.graduacoes) : loadingState()}
        </section>

        <section class="danger-zone">
          <div>
            <h2>Excluir cadastro</h2>
            <p>Só é possível para alunos sem histórico financeiro. Quem já pagou deve ficar como Inativo.</p>
          </div>
          <button class="button ghost danger-text" data-action="delete" data-id="${a.id}">${icon('trash', 18)} Excluir aluno</button>
        </section>
      </div>

      <section class="panel">
        <div class="panel-heading">
          <div>
            <h2>Mensalidades</h2>
            <p>${d ? `${pagos.length} pagas · ${abertos.length} em aberto${atrasados.length ? ` · <span class="text-late">${atrasados.length} em atraso</span>` : ''}` : 'Carregando…'}</p>
          </div>
          ${d && pagos.length ? `<div class="panel-total"><small>Total pago</small><strong>${money(pagos.reduce((s, p) => s + Number(p.valor), 0))}</strong></div>` : ''}
        </div>
        ${d ? historico(d.pagamentos) : loadingState()}
      </section>
    </div>
  </main>`;
}
