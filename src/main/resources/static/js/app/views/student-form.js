/* Cadastro e edição de aluno. */
import { esc, icon } from '../../common.js';
import { FAIXAS, MODALIDADES, todayIso } from '../format.js';
import { findAluno } from '../state.js';
import { emptyState } from '../ui.js';

const field = (name, label, value, attrs = '', hint = '') => `
  <label class="field" data-field="${name}">
    <span class="field-label">${label}</span>
    <span class="input-wrap"><input name="${name}" value="${esc(value ?? '')}" ${attrs}></span>
    ${hint ? `<span class="field-hint">${hint}</span>` : ''}
    <span class="error-text" hidden></span>
  </label>`;

export function studentFormPage(id) {
  const aluno = id ? findAluno(id) : null;
  if (id && !aluno) {
    return `<main class="page">
      <a class="back-link" href="#/alunos">${icon('back', 18)} Alunos</a>
      ${emptyState('search', 'Aluno não encontrado', 'O cadastro pode ter sido excluído.')}
    </main>`;
  }
  const a = aluno || {
    nome: '', telefone: '', modalidade: '', turmaHorario: '', faixaAtual: '',
    valorMensalidade: 180, status: 'ATIVO', diaVencimento: 10, dataMatricula: todayIso(),
  };
  const voltar = aluno ? `#/alunos/${aluno.id}` : '#/alunos';
  const opcional = '<small>(opcional)</small>';

  return `<main class="page">
    <a class="back-link" href="${voltar}">${icon('back', 18)} ${aluno ? esc(aluno.nome) : 'Alunos'}</a>
    <div class="page-header"><div>
      <h1>${aluno ? 'Editar aluno' : 'Novo aluno'}</h1>
      <p>${aluno ? 'Atualize os dados da matrícula.' : 'Preencha os dados para organizar a matrícula.'}</p>
    </div></div>

    <form class="panel student-form" id="student-form" data-id="${aluno ? aluno.id : ''}" novalidate>
      <fieldset class="form-section">
        <legend>Contato</legend>
        ${field('nome', 'Nome completo', a.nome, 'placeholder="Ex.: Lucas Almeida" autocomplete="off" maxlength="150" required')}
        ${field('telefone', `Telefone / WhatsApp ${opcional}`, a.telefone, 'type="tel" inputmode="tel" placeholder="(00) 00000-0000" autocomplete="off"')}
      </fieldset>

      <fieldset class="form-section">
        <legend>Treino</legend>
        <label class="field" data-field="modalidade">
          <span class="field-label">Modalidade</span>
          <span class="select-wrap">
            <select name="modalidade" required>
              <option value="">Selecione uma modalidade</option>
              ${Object.entries(MODALIDADES).map(([k, v]) => `<option value="${k}" ${a.modalidade === k ? 'selected' : ''}>${v}</option>`).join('')}
            </select>
            ${icon('chevron', 16)}
          </span>
          <span class="error-text" hidden></span>
        </label>
        <div class="field-row">
          ${field('turmaHorario', `Turma / horário ${opcional}`, a.turmaHorario, 'placeholder="Ex.: Seg / Qua · 19h" maxlength="100"')}
          ${field('faixaAtual', `Faixa atual ${opcional}`, a.faixaAtual, 'list="faixas-form" placeholder="Ex.: Branca" maxlength="50"',
            aluno ? 'Para guardar o histórico, registre trocas de faixa na ficha do aluno.' : '')}
        </div>
        <datalist id="faixas-form">${FAIXAS.map((f) => `<option value="${f}">`).join('')}</datalist>
      </fieldset>

      <fieldset class="form-section">
        <legend>Mensalidade</legend>
        <div class="field-row three">
          ${field('valorMensalidade', 'Valor (R$)', a.valorMensalidade, 'type="number" inputmode="decimal" min="0.01" step="0.01" required')}
          <label class="field" data-field="diaVencimento">
            <span class="field-label">Dia do vencimento</span>
            <span class="select-wrap">
              <select name="diaVencimento">
                ${Array.from({ length: 28 }, (_, i) => i + 1).map((d) => `<option value="${d}" ${Number(a.diaVencimento) === d ? 'selected' : ''}>Dia ${d}</option>`).join('')}
              </select>
              ${icon('chevron', 16)}
            </span>
            <span class="error-text" hidden></span>
          </label>
          ${field('dataMatricula', 'Data da matrícula', a.dataMatricula, `type="date" max="${todayIso()}"`)}
        </div>
        <fieldset class="status-field">
          <legend>Status do aluno</legend>
          <label><input type="radio" name="status" value="ATIVO" ${a.status === 'ATIVO' ? 'checked' : ''}><span></span>Ativo</label>
          <label><input type="radio" name="status" value="INATIVO" ${a.status === 'INATIVO' ? 'checked' : ''}><span></span>Inativo</label>
          <p>Somente alunos ativos recebem mensalidades. Mensalidades em aberto acompanham mudanças de valor e de vencimento.</p>
        </fieldset>
      </fieldset>

      <div class="form-alert" id="form-alert" hidden></div>

      <div class="form-actions">
        <a class="button ghost" href="${voltar}">Cancelar</a>
        <button class="button primary" type="submit">${icon('check', 18)} ${aluno ? 'Salvar alterações' : 'Cadastrar aluno'}</button>
      </div>
    </form>
  </main>`;
}

/** Lê e valida o formulário com as mesmas regras do servidor. */
export function readStudentForm(form) {
  const data = Object.fromEntries(new FormData(form).entries());
  const dados = {
    nome: data.nome.trim(),
    telefone: data.telefone.trim(),
    modalidade: data.modalidade || null,
    turmaHorario: data.turmaHorario.trim(),
    faixaAtual: data.faixaAtual.trim(),
    valorMensalidade: data.valorMensalidade ? Number(data.valorMensalidade) : null,
    status: data.status || 'ATIVO',
    diaVencimento: Number(data.diaVencimento) || 10,
    dataMatricula: data.dataMatricula || null,
  };
  const digitos = dados.telefone.replace(/\D/g, '').length;
  const errors = {
    nome: dados.nome.length < 3 ? 'Informe o nome completo.' : '',
    telefone: dados.telefone && (digitos < 10 || digitos > 11) ? 'Informe um telefone com DDD.' : '',
    modalidade: !dados.modalidade ? 'Selecione a modalidade.' : '',
    valorMensalidade: !dados.valorMensalidade || dados.valorMensalidade <= 0 ? 'Informe uma mensalidade válida.' : '',
    dataMatricula: dados.dataMatricula && dados.dataMatricula > todayIso() ? 'A matrícula não pode ser no futuro.' : '',
  };
  return { dados, errors };
}
