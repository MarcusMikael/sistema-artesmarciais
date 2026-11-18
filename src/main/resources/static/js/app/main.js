/* Painel administrativo — Escola de Artes Marciais Alessandro Silva */
import { api, esc, icon } from '../common.js';
import { capital, downloadCsv, formatDate, formatDateTime, formatPhone, mesNome, modalidade } from './format.js';
import {
  findAluno, findPagamento, loadAlunos, loadDetalhe, loadPagamentos, loadReceita, loadUser,
  pagamentosDoMes, situacao, state,
} from './state.js';
import { shell, toast } from './ui.js';
import { dashboardPage } from './views/dashboard.js';
import { financePage, paymentResults } from './views/finance.js';
import { reportsPage } from './views/reports.js';
import { studentDetailPage } from './views/student-detail.js';
import { readStudentForm, studentFormPage } from './views/student-form.js';
import { studentResults, studentsPage } from './views/students.js';

const root = document.getElementById('app');

/* ---------- Rotas ---------- */

function route() {
  const parts = location.hash.replace(/^#\/?/, '').split('/').filter(Boolean);
  if (parts[0] === 'alunos') {
    if (parts[1] === 'novo') return { screen: 'student-form', nav: 'students', id: null };
    if (parts[1] && parts[2] === 'editar') return { screen: 'student-form', nav: 'students', id: Number(parts[1]) };
    if (parts[1]) return { screen: 'student-detail', nav: 'students', id: Number(parts[1]) };
    return { screen: 'students', nav: 'students' };
  }
  if (parts[0] === 'financeiro') return { screen: 'finance', nav: 'finance' };
  if (parts[0] === 'relatorios') return { screen: 'reports', nav: 'reports' };
  return { screen: 'dashboard', nav: 'dashboard' };
}

const SCREENS = {
  dashboard: () => dashboardPage(),
  students: () => studentsPage(),
  'student-detail': (r) => studentDetailPage(r.id),
  'student-form': (r) => studentFormPage(r.id),
  finance: () => financePage(),
  reports: () => reportsPage(),
};

const TITLES = {
  dashboard: 'Início', students: 'Alunos', 'student-detail': 'Aluno', 'student-form': 'Aluno',
  finance: 'Financeiro', reports: 'Relatórios',
};

function render() {
  const r = route();
  root.innerHTML = shell(r.nav, SCREENS[r.screen](r));
  document.title = `${TITLES[r.screen]} · Alessandro Silva`;
}

/** Carrega o que a tela precisa e desenha (primeiro com o que já existe). */
async function navigate() {
  const r = route();
  render();
  try {
    if ((r.screen === 'dashboard' || r.screen === 'reports') && !state.receita.length) {
      await loadReceita();
      render();
    }
    if (r.screen === 'student-detail' && (!state.detalhe || state.detalhe.id !== r.id)) {
      state.detalhe = null;
      await loadDetalhe(r.id);
      if (route().screen === 'student-detail') render();
    }
  } catch (err) { handleError(err); }
}

/** Recarrega os dados depois de uma alteração. */
async function reload() {
  const r = route();
  await Promise.all([
    loadAlunos(),
    loadPagamentos(),
    loadReceita(),
    r.screen === 'student-detail' ? loadDetalhe(r.id) : Promise.resolve(),
  ]);
  render();
}

function handleError(err) {
  if (err && err.status === 401) {
    location.replace('/login.html');
    return;
  }
  toast((err && err.message) || 'Algo deu errado. Tente novamente.', 'error');
}

async function run(action, successMessage) {
  if (state.busy) return;
  state.busy = true;
  document.body.classList.add('is-busy');
  try {
    await action();
    await reload();
    if (successMessage) toast(successMessage);
  } catch (err) {
    handleError(err);
  } finally {
    state.busy = false;
    document.body.classList.remove('is-busy');
  }
}

async function changeMonth(delta) {
  let { mes, ano } = state.ref;
  mes += delta;
  if (mes < 1) { mes = 12; ano -= 1; }
  if (mes > 12) { mes = 1; ano += 1; }
  state.ref = { mes, ano };
  try {
    await loadPagamentos();
    render();
  } catch (err) { handleError(err); }
}

/* ---------- Formulários ---------- */

function setFieldError(form, name, message) {
  const field = form.querySelector(`[data-field="${name}"]`);
  if (!field) return;
  const text = field.querySelector('.error-text');
  field.classList.toggle('field-error', !!message);
  text.hidden = !message;
  text.textContent = message || '';
}

function showErrors(form, errors) {
  Object.entries(errors).forEach(([k, v]) => setFieldError(form, k, v));
  const first = Object.keys(errors).find((k) => errors[k]);
  if (first && form.elements[first]) form.elements[first].focus();
  return !!first;
}

async function saveStudent(form) {
  const { dados, errors } = readStudentForm(form);
  if (showErrors(form, errors)) return;

  const id = form.dataset.id;
  const submit = form.querySelector('[type="submit"]');
  const alert = form.querySelector('#form-alert');
  submit.disabled = true;
  alert.hidden = true;
  try {
    const salvo = id
      ? await api(`/api/alunos/${id}`, { method: 'PUT', body: dados })
      : await api('/api/alunos', { method: 'POST', body: dados });
    await Promise.all([loadAlunos(), loadPagamentos(), loadReceita()]);
    state.detalhe = null;
    location.hash = `/alunos/${salvo.id}`;
    toast(id ? 'Dados do aluno atualizados.' : 'Aluno cadastrado com sucesso.');
  } catch (err) {
    submit.disabled = false;
    if (err.body && err.body.fields) showErrors(form, err.body.fields);
    if (err.status === 401) { handleError(err); return; }
    alert.innerHTML = `${icon('warning', 17)}${esc(err.message)}`;
    alert.hidden = false;
  }
}

async function saveGraduacao(form) {
  const data = Object.fromEntries(new FormData(form).entries());
  const errors = {
    faixa: !data.faixa.trim() ? 'Informe a faixa.' : '',
    data: !data.data ? 'Informe a data.' : '',
  };
  if (showErrors(form, errors)) return;
  const { id } = route();
  await run(() => api(`/api/alunos/${id}/graduacoes`, {
    method: 'POST',
    body: { faixa: data.faixa.trim(), data: data.data, observacao: data.observacao.trim() },
  }), 'Graduação registrada.');
}

/* ---------- Exportações ---------- */

const STATUS_LABEL = { paid: 'Pago', waiting: 'Aguardando confirmação', pending: 'Pendente', late: 'Atrasado' };

function exportStudents() {
  const rows = state.alunos.slice().sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')).map((a) => [
    a.nome, a.telefone, modalidade(a.modalidade), a.turmaHorario, a.faixaAtual,
    Number(a.valorMensalidade), a.diaVencimento, formatDate(a.dataMatricula), a.status === 'ATIVO' ? 'Ativo' : 'Inativo',
  ]);
  downloadCsv('alunos.csv',
    ['Nome', 'Telefone', 'Modalidade', 'Turma', 'Faixa', 'Mensalidade', 'Dia vencimento', 'Matrícula', 'Status'], rows);
}

function exportPayments() {
  const { mes, ano } = state.ref;
  const rows = pagamentosDoMes().slice().sort((a, b) => a.aluno.nome.localeCompare(b.aluno.nome, 'pt-BR')).map((p) => [
    p.aluno.nome, p.aluno.telefone, modalidade(p.aluno.modalidade), `${String(mes).padStart(2, '0')}/${ano}`,
    formatDate(p.dataVencimento), Number(p.valor), STATUS_LABEL[situacao(p)], p.diasAtraso || '',
    formatDateTime(p.dataHoraConfirmacao),
  ]);
  downloadCsv(`mensalidades-${ano}-${String(mes).padStart(2, '0')}.csv`,
    ['Aluno', 'Telefone', 'Modalidade', 'Referência', 'Vencimento', 'Valor', 'Situação', 'Dias de atraso', 'Confirmado em'], rows);
}

function exportReport() {
  const rows = state.receita.map((m) => [
    capital(mesNome(m.mes)), m.ano, Number(m.previsto), Number(m.recebido), Number(m.aguardando),
    Number(m.pendente), m.quantidade, m.quantidadePagos,
  ]);
  downloadCsv('receita-mensal.csv',
    ['Mês', 'Ano', 'Cobrado', 'Recebido', 'Aguardando', 'Em aberto', 'Mensalidades', 'Pagas'], rows);
}

/* ---------- Diálogos ---------- */

async function modalOk() {
  if (state.busy) return;
  const m = state.modal;
  state.modal = null;
  render();
  if (m.type === 'confirm') {
    await run(() => api(`/api/pagamentos/${m.pagamento.id}/confirmar`, { method: 'PATCH' }), 'Pagamento confirmado.');
  } else if (m.type === 'undo') {
    await run(() => api(`/api/pagamentos/${m.pagamento.id}/desfazer-confirmacao`, { method: 'PATCH' }), 'Confirmação desfeita.');
  } else if (m.type === 'undo-receipt') {
    await run(() => api(`/api/pagamentos/${m.pagamento.id}/desfazer-comprovante`, { method: 'PATCH' }), 'Mensalidade voltou para pendente.');
  } else if (m.type === 'delete-grad') {
    await run(() => api(`/api/alunos/${m.alunoId}/graduacoes/${m.graduacao.id}`, { method: 'DELETE' }), 'Graduação removida.');
  } else if (m.type === 'delete') {
    try {
      await api(`/api/alunos/${m.aluno.id}`, { method: 'DELETE' });
      await Promise.all([loadAlunos(), loadPagamentos(), loadReceita()]);
      state.detalhe = null;
      location.hash = '/alunos';
      toast('Aluno excluído.');
    } catch (err) { handleError(err); }
  }
}

function openModal(modal) {
  state.modal = modal;
  render();
  const first = root.querySelector('.modal #modal-check, .modal [data-action="modal-ok"]');
  if (first) first.focus();
}

/* ---------- Eventos ---------- */

const ACTIONS = {
  'toggle-menu': () => { state.menuOpen = !state.menuOpen; render(); },
  logout: async () => {
    try { await api('/api/auth/logout', { method: 'POST' }); } catch { /* sessão já expirada */ }
    location.replace('/login.html');
  },
  'month-prev': () => changeMonth(-1),
  'month-next': () => changeMonth(1),
  'students-filter': (el) => { state.students.status = el.dataset.filter; render(); },
  'finance-filter': (el) => { state.finance.filter = el.dataset.filter; render(); },
  'report-period': async (el) => {
    state.receitaMeses = Number(el.dataset.meses);
    try { await loadReceita(); render(); } catch (err) { handleError(err); }
  },
  confirm: (el) => openModal({ type: 'confirm', pagamento: findPagamento(el.dataset.id) }),
  undo: (el) => openModal({ type: 'undo', pagamento: findPagamento(el.dataset.id) }),
  'undo-receipt': (el) => openModal({ type: 'undo-receipt', pagamento: findPagamento(el.dataset.id) }),
  delete: (el) => openModal({ type: 'delete', aluno: findAluno(el.dataset.id) }),
  'delete-grad': (el) => {
    const graduacao = state.detalhe.graduacoes.find((g) => g.id === Number(el.dataset.id));
    openModal({ type: 'delete-grad', graduacao, alunoId: state.detalhe.id });
  },
  receipt: (el) => run(() => api(`/api/pagamentos/${el.dataset.id}/comprovante`, { method: 'PATCH' }),
    'Comprovante registrado. Falta conferir.'),
  generate: () => {
    const { mes, ano } = state.ref;
    run(() => api(`/api/pagamentos/gerar?mes=${mes}&ano=${ano}`, { method: 'POST' }), 'Mensalidades geradas.');
  },
  'export-students': exportStudents,
  'export-payments': exportPayments,
  'export-report': exportReport,
  'modal-cancel': () => { state.modal = null; render(); },
  'modal-ok': () => modalOk(),
};

root.addEventListener('click', (event) => {
  const filterLink = event.target.closest('[data-filter-link]');
  if (filterLink) state.finance.filter = filterLink.dataset.filterLink;

  const el = event.target.closest('[data-action]');
  if (!el || !ACTIONS[el.dataset.action]) return;
  event.preventDefault();
  ACTIONS[el.dataset.action](el);
});

root.addEventListener('change', (event) => {
  const t = event.target;
  if (t.id === 'modal-check') {
    root.querySelector('[data-action="modal-ok"]').disabled = !t.checked;
  } else if (t.dataset.change === 'students-modalidade') {
    state.students.modalidade = t.value;
    document.getElementById('student-results').innerHTML = studentResults();
  } else if (t.dataset.change === 'students-sort') {
    state.students.sort = t.value;
    document.getElementById('student-results').innerHTML = studentResults();
  }
});

root.addEventListener('input', (event) => {
  const t = event.target;
  if (t.id === 'students-search') {
    state.students.query = t.value;
    document.getElementById('student-results').innerHTML = studentResults();
  } else if (t.id === 'finance-search') {
    state.finance.query = t.value;
    document.getElementById('payment-results').innerHTML = paymentResults();
  } else if (t.name === 'telefone') {
    t.value = formatPhone(t.value);
  }
  const field = t.closest('.field-error');
  if (field) {
    field.classList.remove('field-error');
    field.querySelector('.error-text').hidden = true;
  }
});

root.addEventListener('submit', (event) => {
  event.preventDefault();
  if (event.target.id === 'student-form') saveStudent(event.target);
  if (event.target.id === 'grad-form') saveGraduacao(event.target);
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && (state.modal || state.menuOpen)) {
    state.modal = null;
    state.menuOpen = false;
    render();
  }
});

window.addEventListener('hashchange', () => {
  state.menuOpen = false;
  state.modal = null;
  window.scrollTo(0, 0);
  navigate();
});

/* ---------- Inicialização ---------- */

(async function init() {
  try {
    await loadUser();
    await Promise.all([loadAlunos(), loadPagamentos()]);
    navigate();
  } catch (err) {
    if (err && err.status === 401) location.replace('/login.html');
    else {
      root.innerHTML = `<div class="empty-state">${icon('warning', 28)}<h2>Não foi possível carregar o painel</h2><p>${esc(err.message)}</p></div>`;
    }
  }
})();
