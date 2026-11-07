/* Utilitários compartilhados entre a tela de login e o painel. */

const ICONS = {
  arrow: '<path d="M5 12h14"/><path d="m14 7 5 5-5 5"/>',
  back: '<path d="m15 18-6-6 6-6"/><path d="M9 12h10"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  chevron: '<path d="m9 18 6-6-6-6"/>',
  dashboard: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
  edit: '<path d="M12 20h9"/><path d="m16.5 3.5 4 4L8 20l-5 1 1-5Z"/>',
  eye: '<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/>',
  eyeOff: '<path d="m3 3 18 18"/><path d="M10.6 10.6a2 2 0 0 0 2.8 2.8M9.8 5.2A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.1 3M6.6 6.6C3.7 8.4 2 12 2 12s3.5 7 10 7a10 10 0 0 0 4-.8"/>',
  finance: '<rect x="2.5" y="5" width="19" height="14" rx="2"/><path d="M2.5 9h19M7 15h4"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7h.01"/>',
  lock: '<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
  logout: '<path d="M10 17l5-5-5-5M15 12H3"/><path d="M14 3h5a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-5"/>',
  mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  receipt: '<path d="M6 2h9l4 4v16l-3-2-3 2-3-2-3 2-2-1V3Z"/><path d="M14 2v5h5M9 11h6M9 15h6"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
  students: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
  trash: '<path d="M3 6h18M8 6V4h8v2M19 6l-1 15H6L5 6M10 11v6M14 11v6"/>',
  undo: '<path d="m9 7-5 5 5 5"/><path d="M4 12h9a7 7 0 1 1 0 14"/>',
  warning: '<path d="M12 3 2 21h20Z"/><path d="M12 9v5M12 18h.01"/>',
  x: '<path d="m6 6 12 12M18 6 6 18"/>',
};

function icon(name, size = 20) {
  return `<svg aria-hidden="true" class="icon" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${ICONS[name] || ''}</svg>`;
}

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

const moneyFormat = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
function money(value) {
  return moneyFormat.format(Number(value) || 0);
}

function brandHtml() {
  return `<div class="brand">
      <img class="brand-logo" src="/img/logo.webp" alt="">
      <span class="brand-text">
        <span>Alessandro <b>Silva</b></span>
        <small>Escola de Artes Marciais</small>
      </span>
    </div>`;
}

/* ---------- Comunicação com a API (sessão + CSRF) ---------- */

function readCookie(name) {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
  return match ? decodeURIComponent(match[1]) : null;
}

async function csrfToken() {
  let token = readCookie('XSRF-TOKEN');
  if (!token) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' });
    token = readCookie('XSRF-TOKEN');
  }
  return token;
}

class ApiError extends Error {
  constructor(status, body) {
    super((body && body.message) || 'Não foi possível concluir a operação.');
    this.status = status;
    this.body = body;
  }
}

async function api(path, { method = 'GET', body, form } = {}) {
  const headers = { Accept: 'application/json' };
  const options = { method, headers, credentials: 'same-origin' };

  if (method !== 'GET') {
    headers['X-XSRF-TOKEN'] = await csrfToken();
  }
  if (form) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded';
    options.body = new URLSearchParams(form);
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response = await fetch(path, options);

  // Token CSRF expirado/rotacionado: busca um novo e tenta mais uma vez.
  if (response.status === 403 && method !== 'GET') {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/';
    headers['X-XSRF-TOKEN'] = await csrfToken();
    if (form) options.body = new URLSearchParams(form);
    response = await fetch(path, options);
  }

  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }

  if (!response.ok) throw new ApiError(response.status, data);
  return data;
}
