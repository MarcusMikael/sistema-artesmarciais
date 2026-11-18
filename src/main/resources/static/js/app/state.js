/* Estado do painel e carregamento de dados da API. */
import { api } from '../common.js';

const agora = new Date();

export const state = {
  user: null,
  alunos: [],
  pagamentos: [],            // mensalidades do mês de referência
  ref: { mes: agora.getMonth() + 1, ano: agora.getFullYear() },
  receita: [],               // últimos meses, do mais antigo para o mais recente
  receitaMeses: 12,
  detalhe: null,             // { id, pagamentos, graduacoes } do aluno aberto
  students: { query: '', status: 'ATIVO', modalidade: '', sort: 'nome' },
  finance: { query: '', filter: 'all' },
  menuOpen: false,
  modal: null,
  busy: false,
};

export const isCurrentRef = () => {
  const d = new Date();
  return state.ref.mes === d.getMonth() + 1 && state.ref.ano === d.getFullYear();
};

export const ativos = () => state.alunos.filter((a) => a.status === 'ATIVO');

/** Mensalidades que contam no mês: alunos ativos, ou já com comprovante/pagas. */
export const pagamentosDoMes = () => state.pagamentos.filter(
  (p) => p.aluno && (p.aluno.status === 'ATIVO' || p.statusPagamento !== 'PENDENTE'));

/** Situação para exibição: late (atrasado), pending, waiting, paid. */
export function situacao(p) {
  if (p.statusPagamento === 'PAGO') return 'paid';
  if (p.statusPagamento === 'AGUARDANDO_CONFIRMACAO') return 'waiting';
  return p.atrasado ? 'late' : 'pending';
}

export function resumo(pagamentos) {
  const r = { total: 0, previsto: 0, recebido: 0, aguardando: 0, aberto: 0, atrasado: 0,
    nPaid: 0, nWaiting: 0, nPending: 0, nLate: 0 };
  pagamentos.forEach((p) => {
    const v = Number(p.valor) || 0;
    const s = situacao(p);
    r.total += 1;
    r.previsto += v;
    if (s === 'paid') { r.recebido += v; r.nPaid += 1; }
    if (s === 'waiting') { r.aguardando += v; r.nWaiting += 1; }
    if (s === 'pending' || s === 'late') { r.aberto += v; r.nPending += 1; }
    if (s === 'late') { r.atrasado += v; r.nLate += 1; }
  });
  return r;
}

export function findPagamento(id) {
  const n = Number(id);
  return state.pagamentos.find((p) => p.id === n)
    || (state.detalhe && state.detalhe.pagamentos.find((p) => p.id === n));
}

export const findAluno = (id) => state.alunos.find((a) => a.id === Number(id));

/* ---------- Carregamento ---------- */

export async function loadUser() {
  state.user = await api('/api/auth/me');
}

export async function loadAlunos() {
  state.alunos = await api('/api/alunos');
}

export async function loadPagamentos() {
  const { mes, ano } = state.ref;
  // O mês atual tem as mensalidades geradas automaticamente.
  state.pagamentos = isCurrentRef()
    ? await api(`/api/pagamentos/gerar?mes=${mes}&ano=${ano}`, { method: 'POST' })
    : await api(`/api/pagamentos/mes?mes=${mes}&ano=${ano}`);
}

export async function loadReceita() {
  state.receita = await api(`/api/relatorios/receita?meses=${state.receitaMeses}`);
}

export async function loadDetalhe(id) {
  const [pagamentos, graduacoes] = await Promise.all([
    api(`/api/alunos/${id}/pagamentos`),
    api(`/api/alunos/${id}/graduacoes`),
  ]);
  state.detalhe = { id, pagamentos, graduacoes };
}
