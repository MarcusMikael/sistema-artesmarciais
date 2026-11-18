/* Lista de alunos: tabela no computador, lista compacta no celular. */
import { esc, icon, money } from '../../common.js';
import { MODALIDADES, beltStyle, modalidade, norm } from '../format.js';
import { ativos, state } from '../state.js';
import { avatar, emptyState, pageHeader, statusChip, whatsappButton } from '../ui.js';

const SORTS = {
  nome: (a, b) => a.nome.localeCompare(b.nome, 'pt-BR'),
  valor: (a, b) => Number(b.valorMensalidade) - Number(a.valorMensalidade),
  vencimento: (a, b) => a.diaVencimento - b.diaVencimento || a.nome.localeCompare(b.nome, 'pt-BR'),
  matricula: (a, b) => String(b.dataMatricula || '').localeCompare(String(a.dataMatricula || '')),
};

export function filteredStudents() {
  const f = state.students;
  const q = norm(f.query);
  return state.alunos
    .filter((a) => (f.status === 'all' || a.status === f.status)
      && (!f.modalidade || a.modalidade === f.modalidade)
      && [a.nome, a.telefone, modalidade(a.modalidade), a.faixaAtual, a.turmaHorario].some((v) => norm(v).includes(q)))
    .sort(SORTS[f.sort] || SORTS.nome);
}

export function faixaBadge(faixa) {
  if (!faixa) return '<span class="muted">—</span>';
  const belt = beltStyle(faixa);
  return `<span class="belt">${belt ? `<i style="${belt}"></i>` : ''}${esc(faixa)}</span>`;
}

function studentRows(list) {
  return `<div class="table-wrap desktop-only">
    <table class="data-table">
      <thead><tr>
        <th>Aluno</th><th>Modalidade</th><th>Turma</th><th>Faixa</th>
        <th class="num">Vence</th><th class="num">Mensalidade</th><th>Status</th><th><span class="sr-only">Ações</span></th>
      </tr></thead>
      <tbody>
        ${list.map((a) => `<tr>
          <td><a class="cell-person" href="#/alunos/${a.id}">${avatar(a.nome, 'sm', a.faixaAtual)}<span><strong>${esc(a.nome)}</strong><small>${esc(a.telefone || 'Sem telefone')}</small></span></a></td>
          <td>${esc(modalidade(a.modalidade))}</td>
          <td>${esc(a.turmaHorario || '—')}</td>
          <td>${faixaBadge(a.faixaAtual)}</td>
          <td class="num">Dia ${a.diaVencimento}</td>
          <td class="num">${money(a.valorMensalidade)}</td>
          <td>${statusChip(a.status)}</td>
          <td class="cell-actions">
            ${whatsappButton(a.telefone, `Olá, ${a.nome.split(' ')[0]}!`, '', 'icon-button whatsapp')}
            <a class="icon-button" href="#/alunos/${a.id}/editar" aria-label="Editar ${esc(a.nome)}" title="Editar">${icon('edit', 18)}</a>
          </td>
        </tr>`).join('')}
      </tbody>
    </table>
  </div>
  <ul class="list-card mobile-only">
    ${list.map((a) => `<li class="list-item">
      <a class="list-row" href="#/alunos/${a.id}">
        ${avatar(a.nome, '', a.faixaAtual)}
        <span class="list-main">
          <strong>${esc(a.nome)}</strong>
          <small>${esc(modalidade(a.modalidade))}${a.turmaHorario ? ` · ${esc(a.turmaHorario)}` : ''}</small>
          <small>${esc(a.telefone || 'Sem telefone')}${a.faixaAtual ? ` · Faixa ${esc(a.faixaAtual)}` : ''}</small>
        </span>
        <span class="list-side">
          ${statusChip(a.status)}
          <strong>${money(a.valorMensalidade)}</strong>
        </span>
      </a>
      <a class="icon-button list-edit" href="#/alunos/${a.id}/editar" aria-label="Editar ${esc(a.nome)}" title="Editar">${icon('edit', 18)}</a>
    </li>`).join('')}
  </ul>`;
}

export function studentResults() {
  const list = filteredStudents();
  if (!list.length) {
    return emptyState('search', 'Nenhum aluno encontrado', 'Tente ajustar a busca ou os filtros.');
  }
  return `${studentRows(list)}<p class="result-count">${list.length} ${list.length === 1 ? 'aluno' : 'alunos'}</p>`;
}

export function studentsPage() {
  const f = state.students;
  const total = state.alunos.length;
  const nAtivos = ativos().length;
  const seg = (key, label, n) => `<button class="${f.status === key ? 'selected' : ''}" data-action="students-filter" data-filter="${key}" aria-pressed="${f.status === key}">${label} <b>${n}</b></button>`;

  return `<main class="page page-wide">
    ${pageHeader('Alunos', `${total} ${total === 1 ? 'aluno cadastrado' : 'alunos cadastrados'} · ${nAtivos} ${nAtivos === 1 ? 'ativo' : 'ativos'}`,
      `<button class="button outline" data-action="export-students">${icon('download', 18)} <span class="hide-mobile">Exportar</span></button>
       <a class="button primary" href="#/alunos/novo">${icon('plus', 18)} Novo aluno</a>`)}

    <div class="toolbar">
      <label class="input-wrap search">${icon('search', 19)}
        <input id="students-search" type="search" aria-label="Buscar aluno" placeholder="Buscar por nome, telefone, faixa…" value="${esc(f.query)}">
      </label>
      <div class="segmented" role="group" aria-label="Filtrar por status">
        ${seg('ATIVO', 'Ativos', nAtivos)}${seg('INATIVO', 'Inativos', total - nAtivos)}${seg('all', 'Todos', total)}
      </div>
      <div class="toolbar-selects">
        <label class="select-wrap compact">
          <span class="sr-only">Modalidade</span>
          <select data-change="students-modalidade">
            <option value="">Todas as modalidades</option>
            ${Object.entries(MODALIDADES).map(([k, v]) => `<option value="${k}" ${f.modalidade === k ? 'selected' : ''}>${v}</option>`).join('')}
          </select>${icon('chevron', 16)}
        </label>
        <label class="select-wrap compact">
          <span class="sr-only">Ordenar</span>
          <select data-change="students-sort">
            <option value="nome" ${f.sort === 'nome' ? 'selected' : ''}>Ordenar: nome</option>
            <option value="vencimento" ${f.sort === 'vencimento' ? 'selected' : ''}>Ordenar: vencimento</option>
            <option value="valor" ${f.sort === 'valor' ? 'selected' : ''}>Ordenar: mensalidade</option>
            <option value="matricula" ${f.sort === 'matricula' ? 'selected' : ''}>Ordenar: mais recentes</option>
          </select>${icon('chevron', 16)}
        </label>
      </div>
    </div>

    <section class="panel flat" id="student-results">${studentResults()}</section>
  </main>`;
}
