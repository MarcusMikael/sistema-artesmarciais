/* Formatação e utilitários puros (sem estado). */
import { money } from '../common.js';

export const MODALIDADES = {
  JIU_JITSU: 'Jiu-jítsu',
  LUTA_LIVRE: 'Luta Livre',
  KICKBOXING: 'Kickboxing',
};

export const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho',
  'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

export const FAIXAS = ['Branca', 'Cinza', 'Amarela', 'Laranja', 'Verde', 'Azul', 'Roxa', 'Marrom', 'Preta'];

const CORES_FAIXA = {
  branca: '#f2f2f2', cinza: '#9e9e9e', amarela: '#fdd835', laranja: '#fb8c00', verde: '#43a047',
  azul: '#1e88e5', roxa: '#8e24aa', marrom: '#795548', preta: '#0a0a0a', vermelha: '#e53935', coral: '#ff7043',
};

export const modalidade = (m) => MODALIDADES[m] || m || '—';
export const capital = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : '');
export const mesNome = (mes) => MESES[mes - 1];
export const mesCurto = (mes) => capital(MESES[mes - 1].slice(0, 3));
export const firstName = (nome) => String(nome || '').trim().split(/\s+/)[0] || '';

export function initials(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return parts.slice(0, 2).map((p) => p[0]).join('').toUpperCase();
}

/** Texto em minúsculas e sem acentos, para buscas. */
export const norm = (s) => String(s || '').toLocaleLowerCase('pt-BR').normalize('NFD').replace(/[̀-ͯ]/g, '');

/** Cor do ponto da faixa (primeira cor reconhecida no texto, ex.: "Azul ponta preta"). */
export function corFaixa(faixa) {
  const n = norm(faixa);
  const chave = Object.keys(CORES_FAIXA).find((k) => n.includes(k));
  return chave ? CORES_FAIXA[chave] : null;
}

/** Variáveis CSS para desenhar a faixa: cor e ponteira (vermelha na preta, preta nas demais). */
export function beltStyle(faixa) {
  const cor = corFaixa(faixa);
  if (!cor) return '';
  const tip = cor === CORES_FAIXA.preta ? 'var(--red)' : '#0a0a0a';
  return `--belt:${cor};--belt-tip:${tip}`;
}

/* ---------- Datas ---------- */

/** "2026-11-05" → Date local (sem problema de fuso). */
export function parseDate(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || '');
  return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : null;
}

export function formatDate(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || '');
  return m ? `${m[3]}/${m[2]}/${m[1]}` : '';
}

export function formatDayMonth(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || '');
  return m ? `${m[3]}/${m[2]}` : '';
}

export function formatDateTime(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(iso || '');
  return m ? `${m[3]}/${m[2]}/${m[1]} às ${m[4]}:${m[5]}` : '';
}

export function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** "há 1 ano e 3 meses" a partir de uma data ISO. */
export function tempoDesde(iso) {
  const d = parseDate(iso);
  if (!d) return '';
  const hoje = new Date();
  let meses = (hoje.getFullYear() - d.getFullYear()) * 12 + hoje.getMonth() - d.getMonth();
  if (hoje.getDate() < d.getDate()) meses -= 1;
  if (meses < 1) return 'há menos de 1 mês';
  const anos = Math.floor(meses / 12);
  const resto = meses % 12;
  const partes = [];
  if (anos) partes.push(`${anos} ${anos === 1 ? 'ano' : 'anos'}`);
  if (resto) partes.push(`${resto} ${resto === 1 ? 'mês' : 'meses'}`);
  return `há ${partes.join(' e ')}`;
}

/* ---------- Telefone e WhatsApp ---------- */

export function formatPhone(value) {
  const d = String(value || '').replace(/\D/g, '').slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : '';
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

export const phoneDigits = (phone) => String(phone || '').replace(/\D/g, '');

/** Link wa.me para o telefone (com DDI 55) ou null se o telefone não tiver DDD. */
export function whatsappLink(phone, text) {
  let d = phoneDigits(phone);
  if (d.length === 10 || d.length === 11) d = `55${d}`;
  if (d.length < 12) return null;
  return `https://wa.me/${d}${text ? `?text=${encodeURIComponent(text)}` : ''}`;
}

export function mensagemCobranca(p) {
  const ref = `${mesNome(p.mesReferencia)}/${p.anoReferencia}`;
  const venc = formatDate(p.dataVencimento);
  const atraso = p.atrasado ? ` (venceu em ${venc})` : ` (vence em ${venc})`;
  return `Olá, ${firstName(p.aluno.nome)}! Tudo bem? Passando para lembrar da mensalidade de ${ref} `
    + `no valor de ${money(p.valor).replace(/ /g, ' ')}${atraso}. Se já pagou, é só me enviar o comprovante por aqui. `
    + 'Obrigado! — Escola de Artes Marciais Alessandro Silva';
}

/* ---------- Exportação CSV (abre direto no Excel em português) ---------- */

export function downloadCsv(filename, header, rows) {
  const cell = (v) => {
    let s = String(v ?? '');
    if (typeof v === 'number') s = Number.isInteger(v) ? String(v) : v.toFixed(2).replace('.', ',');
    // Texto que começa com = + - @ viraria fórmula no Excel: prefixa com apóstrofo.
    else if (/^[=+\-@\t\r]/.test(s)) s = `'${s}`;
    return /[;"\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const csv = [header, ...rows].map((r) => r.map(cell).join(';')).join('\r\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
