/* Painel administrativo — Escola de Artes Marciais Alessandro Silva */
(function () {
  const MODALIDADES = {
    JIU_JITSU: 'Jiu-jítsu',
    LUTA_LIVRE: 'Luta Livre',
    KICKBOXING: 'Kickboxing',
  };
  const PAY_STATUS = { PENDENTE: 'pending', AGUARDANDO_CONFIRMACAO: 'waiting', PAGO: 'paid' };
  const MESES = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho',
    'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];

  let hoje = new Date();
  const state = {
    user: null,
    alunos: [],
    pagamentos: [],
    ref: { mes: hoje.getMonth() + 1, ano: hoje.getFullYear() },
    studentsQuery: '',
    studentsFilter: 'all',
    financeQuery: '',
    financeFilter: 'all',
    menuOpen: false,
    modal: null,
    busy: false,
  };

  const root = document.getElementById('app');
  let toastTimer = null;

  /* ---------- Helpers ---------- */

  const modalidade = (m) => MODALIDADES[m] || m || '—';
  const mesNome = (ref = state.ref) => MESES[ref.mes - 1];
  const capital = (s) => s.charAt(0).toUpperCase() + s.slice(1);
  const isCurrentRef = () => state.ref.mes === hoje.getMonth() + 1 && state.ref.ano === hoje.getFullYear();
  const isFutureRef = () => state.ref.ano * 12 + state.ref.mes > hoje.getFullYear() * 12 + hoje.getMonth() + 1;
  const ativos = () => state.alunos.filter((a) => a.status === 'ATIVO');
  const initials = (name) => {
    const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return parts.slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  };
  const norm = (s) => String(s || '').toLocaleLowerCase('pt-BR').normalize('NFD').replace(/[̀-ͯ]/g, '');

  // Valor gravado no pagamento; registros antigos usam a mensalidade do aluno.
  const valorPagamento = (p) => Number(p.valor ?? p.aluno.valorMensalidade ?? 0);

  // Pagamentos considerados no mês: alunos ativos, ou já com comprovante/pagos.
  const pagamentosVisiveis = () => state.pagamentos.filter(
    (p) => p.aluno && (p.aluno.status === 'ATIVO' || p.statusPagamento !== 'PENDENTE'));

  function formatDateTime(iso) {
    const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(iso || '');
    return m ? `${m[3]}/${m[2]}/${m[1]} às ${m[4]}:${m[5]}` : '';
  }

  function formatPhone(value) {
    const d = value.replace(/\D/g, '').slice(0, 11);
    if (d.length <= 2) return d.length ? `(${d}` : '';
    if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
    if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
    return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  }

  function route() {
    const hash = location.hash.replace(/^#\/?/, '');
    const parts = hash.split('/').filter(Boolean);
    if (parts[0] === 'alunos' && parts[1] === 'novo') return { screen: 'student-form', id: null };
    if (parts[0] === 'alunos' && parts[1]) return { screen: 'student-form', id: Number(parts[1]) };
    if (parts[0] === 'alunos') return { screen: 'students' };
    if (parts[0] === 'financeiro') return { screen: 'finance' };
    return { screen: 'dashboard' };
  }

  function go(path) {
    if (location.hash === '#' + path) render();
    else location.hash = path;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function toast(message, type = 'success') {
    let el = document.getElementById('toast');
    if (!el) {
      el = document.createElement('div');
      el.id = 'toast';
      el.setAttribute('role', 'status');
      document.body.appendChild(el);
    }
    el.className = `toast ${type === 'error' ? 'error' : ''}`;
    el.innerHTML = `<span>${icon(type === 'error' ? 'x' : 'check', 16)}</span>${esc(message)}`;
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, type === 'error' ? 4500 : 2800);
  }

  function handleError(err) {
    if (err && err.status === 401) {
      location.replace('/login.html');
      return;
    }
    toast((err && err.message) || 'Algo deu errado. Tente novamente.', 'error');
  }

  /* ---------- Dados ---------- */

  async function loadAlunos() {
    state.alunos = await api('/api/alunos');
  }

  async function loadPagamentos() {
    hoje = new Date();
    const { mes, ano } = state.ref;
    // O mês atual tem as mensalidades geradas automaticamente.
    state.pagamentos = isCurrentRef()
      ? await api(`/api/pagamentos/gerar?mes=${mes}&ano=${ano}`, { method: 'POST' })
      : await api(`/api/pagamentos/mes?mes=${mes}&ano=${ano}`);
  }

  async function reload() {
    await loadAlunos();
    await loadPagamentos();
    render();
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

  /* ---------- Componentes ---------- */

  function header() {
    const nome = (state.user && state.user.nome) || 'Administrador';
    return `<header class="app-header">
      ${brandHtml()}
      <button class="avatar" data-action="toggle-menu" aria-label="Abrir menu da administração">${esc(initials(nome))}</button>
      ${state.menuOpen ? `
        <button class="menu-scrim" data-action="toggle-menu" aria-label="Fechar menu"></button>
        <div class="profile-menu">
          <div><span>${esc(nome)}</span><small>${esc(state.user ? state.user.email : '')}</small></div>
          <button class="button logout-button" data-action="logout">${icon('logout', 18)} Sair</button>
        </div>` : ''}
    </header>`;
  }

  function bottomNav(screen) {
    const active = screen === 'student-form' ? 'students' : screen;
    const items = [['dashboard', 'dashboard', 'Dashboard', '/dashboard'],
    ['students', 'students', 'Alunos', '/alunos'],
    ['finance', 'finance', 'Financeiro', '/financeiro']];
    return `<nav class="bottom-nav" aria-label="Navegação principal">
      ${items.map(([key, ic, label, path]) => `
        <button class="${active === key ? 'active' : ''}" data-go="${path}">
          ${icon(ic, 21)}<span>${label}</span>
        </button>`).join('')}
    </nav>`;
  }

  function pageIntro(eyebrow, title, description) {
    return `<div class="page-intro">
      <span class="eyebrow">${esc(eyebrow)}</span>
      <h1>${esc(title)}</h1>
      <p>${esc(description)}</p>
    </div>`;
  }

  function monthPicker() {
    return `<div class="month-picker">
      <button class="month-nav prev" data-action="month-prev" aria-label="Mês anterior">${icon('chevron', 18)}</button>
      <span class="month-label">${icon('calendar')}<span>${capital(mesNome())} ${state.ref.ano}</span></span>
      <button class="month-nav next" data-action="month-next" aria-label="Próximo mês">${icon('chevron', 18)}</button>
    </div>`;
  }

  /* ---------- Telas ---------- */

  function dashboardPage() {
    const ativosList = ativos();
    const pays = pagamentosVisiveis();
    const paid = pays.filter((p) => p.statusPagamento === 'PAGO');
    const waiting = pays.filter((p) => p.statusPagamento === 'AGUARDANDO_CONFIRMACAO');
    const pending = pays.filter((p) => p.statusPagamento === 'PENDENTE');
    const total = pays.length || 1;
    const receita = paid.reduce((s, p) => s + valorPagamento(p), 0);

    return `<main class="page dashboard-page">
      ${pageIntro('PAINEL ADMINISTRATIVO', 'Visão geral', 'Sua escola organizada. Uma conferência de cada vez.')}
      ${monthPicker()}
      <section class="metric-grid">
        <article class="metric">
          <div><span>Alunos ativos</span>${icon('students')}</div>
          <strong>${ativosList.length}</strong><small>Matriculados na escola</small>
        </article>
        <article class="metric">
          <div><span>Pagamentos pagos</span>${icon('check')}</div>
          <strong>${paid.length}</strong><small>Conferidos neste mês</small>
        </article>
        <article class="metric yellow">
          <div><span>Aguardando confirmação</span>${icon('calendar')}</div>
          <strong>${waiting.length}</strong><small>Comprovantes recebidos</small>
        </article>
        <article class="metric red">
          <div><span>Pagamentos pendentes</span>${icon('warning')}</div>
          <strong>${pending.length}</strong><small>Sem registro de comprovante</small>
        </article>
      </section>

      <section class="panel ready-panel">
        <div class="panel-heading">
          <div>
            <h2>Prontos para conferência</h2>
            <p>${waiting.length} ${waiting.length === 1 ? 'pagamento aguardando' : 'pagamentos aguardando'} confirmação</p>
          </div>
          <button data-go="/financeiro">Ver todos</button>
        </div>
        ${waiting.length ? `<div class="ready-list">
          ${waiting.slice(0, 5).map((p) => `
            <div class="ready-row">
              <span class="initials">${esc(initials(p.aluno.nome))}</span>
              <div>
                <strong>${esc(p.aluno.nome)}</strong>
                <span>${esc(modalidade(p.aluno.modalidade))} · ${money(valorPagamento(p))}</span>
              </div>
              <button class="button small" data-action="confirm" data-id="${p.id}">Conferir</button>
            </div>`).join('')}
        </div>` : `<div class="empty-inline">${icon('check')} Nenhum comprovante aguardando.</div>`}
        <p class="reference">Referência: ${mesNome()} de ${state.ref.ano}</p>
      </section>

      <section class="panel distribution">
        <h2>Pagamentos do mês</h2>
        <p>${ativosList.length} alunos ativos · ${capital(mesNome())} ${state.ref.ano}</p>
        <div class="status-bar">
          <i class="paid-bar" style="width:${paid.length / total * 100}%"></i>
          <i class="waiting-bar" style="width:${waiting.length / total * 100}%"></i>
          <i class="pending-bar" style="width:${pending.length / total * 100}%"></i>
        </div>
        <ul>
          <li><span><i class="dot green"></i>Pagos</span><strong>${paid.length}</strong></li>
          <li><span><i class="dot yellow-dot"></i>Aguardando confirmação</span><strong>${waiting.length}</strong></li>
          <li><span><i class="dot red-dot"></i>Pendentes</span><strong>${pending.length}</strong></li>
        </ul>
        <div class="revenue"><span>Receita confirmada</span><strong>${money(receita)}</strong></div>
        <button class="text-link" data-go="/financeiro">Abrir financeiro ${icon('arrow', 18)}</button>
      </section>

      <section class="panel new-student">
        <h2>Um novo aluno chegou?</h2>
        <p>Mantenha o cadastro e a mensalidade sempre atualizados.</p>
        <button class="button primary" data-go="/alunos/novo">${icon('plus')} Cadastrar aluno</button>
      </section>
    </main>`;
  }

  function filteredStudents() {
    const q = norm(state.studentsQuery);
    return state.alunos.filter((a) =>
      (state.studentsFilter === 'all' || a.status === state.studentsFilter)
      && [a.nome, a.telefone, modalidade(a.modalidade)].some((v) => norm(v).includes(q)));
  }

  function studentCards() {
    const list = filteredStudents();
    if (!list.length) {
      return `<div class="empty-state">${icon('search', 28)}<h2>Nenhum aluno encontrado</h2><p>Tente ajustar a busca ou os filtros.</p></div>`;
    }
    return list.map((a) => {
      const ativo = a.status === 'ATIVO';
      return `<article class="student-card">
        <div class="student-top">
          <span class="initials">${esc(initials(a.nome))}</span>
          <div class="student-name"><h2>${esc(a.nome)}</h2><p>${esc(a.telefone || 'Sem telefone')}</p></div>
          <span class="status-chip ${ativo ? 'active' : 'inactive'}">${icon(ativo ? 'check' : 'x', 15)}${ativo ? 'Ativo' : 'Inativo'}</span>
        </div>
        <dl>
          <div><dt>MODALIDADE</dt><dd>${esc(modalidade(a.modalidade))}</dd></div>
          <div><dt>TURMA / HORÁRIO</dt><dd>${esc(a.turmaHorario || '—')}</dd></div>
          <div><dt>FAIXA ATUAL</dt><dd>${esc(a.faixaAtual || '—')}</dd></div>
          <div><dt>MENSALIDADE</dt><dd class="nowrap">${money(a.valorMensalidade)}</dd></div>
        </dl>
        <div class="card-actions">
          <button class="button" data-go="/alunos/${a.id}">${icon('edit', 18)} Editar aluno</button>
          <button class="button" data-action="delete" data-id="${a.id}">${icon('trash', 18)} Excluir</button>
        </div>
      </article>`;
    }).join('');
  }

  function studentsPage() {
    const total = state.alunos.length;
    const nAtivos = ativos().length;
    const nInativos = total - nAtivos;
    const seg = (key, label, n) => `<button class="${state.studentsFilter === key ? 'selected' : ''}" data-action="students-filter" data-filter="${key}">${label} <b>${n}</b></button>`;

    return `<main class="page">
      ${pageIntro('PAINEL ADMINISTRATIVO', 'Alunos', `${total} ${total === 1 ? 'aluno cadastrado' : 'alunos cadastrados'} · ${nAtivos} ${nAtivos === 1 ? 'ativo' : 'ativos'} e ${nInativos} ${nInativos === 1 ? 'inativo' : 'inativos'}`)}
      <button class="button primary wide" data-go="/alunos/novo">${icon('plus')} Cadastrar aluno</button>
      <label class="field">
        <span class="input-wrap">${icon('search', 19)}
          <input id="students-search" aria-label="Buscar aluno" placeholder="Buscar por nome, telefone ou modalidade" value="${esc(state.studentsQuery)}">
        </span>
      </label>
      <div class="segmented">
        ${seg('ATIVO', 'Ativos', nAtivos)}${seg('INATIVO', 'Inativos', nInativos)}${seg('all', 'Todos', total)}
      </div>
      <section class="student-list" id="student-list">${studentCards()}</section>
      <p class="info-note">${icon('info', 18)} Somente alunos ativos aparecem no financeiro mensal.</p>
    </main>`;
  }

  function studentFormPage(id) {
    const aluno = id ? state.alunos.find((a) => a.id === id) : null;
    if (id && !aluno) {
      return `<main class="page form-page">
        <button class="back-link" data-go="/alunos">${icon('back', 18)} Voltar para alunos</button>
        <div class="empty-state">${icon('search', 28)}<h2>Aluno não encontrado</h2><p>O cadastro pode ter sido excluído.</p></div>
      </main>`;
    }
    const a = aluno || { nome: '', telefone: '', modalidade: '', turmaHorario: '', faixaAtual: '', valorMensalidade: 180, status: 'ATIVO' };
    const field = (name, label, value, attrs = '') => `
      <label class="field" data-field="${name}">
        <span class="field-label">${label}</span>
        <span class="input-wrap"><input name="${name}" value="${esc(value)}" ${attrs}></span>
        <span class="error-text" hidden></span>
      </label>`;

    return `<main class="page form-page">
      <button class="back-link" data-go="/alunos">${icon('back', 18)} Voltar para alunos</button>
      ${pageIntro('GESTÃO DE ALUNOS', aluno ? 'Editar aluno' : 'Cadastrar aluno', aluno ? 'Atualize os dados da matrícula.' : 'Preencha os dados para organizar a matrícula.')}
      <form class="panel student-form" id="student-form" data-id="${aluno ? aluno.id : ''}" novalidate>
        <div class="form-heading">
          <h2>Dados do aluno</h2>
          <p>Informações de contato, treino e mensalidade.</p>
        </div>
        <div class="form-fields">
          ${field('nome', 'Nome completo', a.nome, 'placeholder="Ex.: Lucas Almeida" autocomplete="off" maxlength="150"')}
          ${field('telefone', 'Telefone / WhatsApp', a.telefone, 'inputmode="tel" placeholder="(00) 00000-0000" autocomplete="off"')}
          <label class="field" data-field="modalidade">
            <span class="field-label">Modalidade</span>
            <span class="select-wrap">
              <select name="modalidade">
                <option value="">Selecione uma modalidade</option>
                ${Object.entries(MODALIDADES).map(([k, v]) => `<option value="${k}" ${a.modalidade === k ? 'selected' : ''}>${v}</option>`).join('')}
              </select>
              ${icon('chevron', 16)}
            </span>
            <span class="error-text" hidden></span>
          </label>
          ${field('turmaHorario', 'Turma / horário', a.turmaHorario, 'placeholder="Ex.: Seg / Qua · 19h" maxlength="100"')}
          ${field('faixaAtual', 'Faixa atual', a.faixaAtual, 'placeholder="Ex.: Branca ou Não se aplica" maxlength="50"')}
          ${field('valorMensalidade', 'Valor da mensalidade', a.valorMensalidade ?? '', 'type="number" inputmode="decimal" min="1" step="0.01"')}
          <fieldset class="status-field">
            <legend>Status do aluno</legend>
            <label><input type="radio" name="status" value="ATIVO" ${a.status === 'ATIVO' ? 'checked' : ''}><span></span>Ativo</label>
            <label><input type="radio" name="status" value="INATIVO" ${a.status === 'INATIVO' ? 'checked' : ''}><span></span>Inativo</label>
            <p>Alunos ativos entram na rotina mensal de pagamentos.</p>
          </fieldset>
        </div>
        <div class="form-actions">
          <p>Revise os dados antes de salvar.</p>
          <button class="button primary" type="submit">${icon('check')} Salvar aluno</button>
          <button class="button" type="button" data-go="/alunos">Cancelar</button>
        </div>
      </form>
    </main>`;
  }

  function filteredPayments() {
    const q = norm(state.financeQuery);
    return pagamentosVisiveis()
      .filter((p) => norm(p.aluno.nome).includes(q)
        && (state.financeFilter === 'all' || PAY_STATUS[p.statusPagamento] === state.financeFilter))
      .sort((x, y) => x.aluno.nome.localeCompare(y.aluno.nome, 'pt-BR'));
  }

  function paymentCards() {
    const list = filteredPayments();
    if (!list.length) {
      const semMensalidades = !pagamentosVisiveis().length && ativos().length && !isCurrentRef();
      return `<div class="empty-state">${icon('finance', 28)}
        <h2>${semMensalidades ? 'Mensalidades ainda não geradas' : 'Nenhum pagamento encontrado'}</h2>
        <p>${semMensalidades ? `Gere as mensalidades de ${mesNome()} para os alunos ativos.` : 'Tente ajustar a busca ou o filtro.'}</p>
        ${semMensalidades ? `<button class="button primary" style="margin-top:18px" data-action="generate">${icon('plus')} Gerar mensalidades</button>` : ''}
      </div>`;
    }
    const ref = `${capital(mesNome())} / ${state.ref.ano}`;
    return list.map((p) => {
      const st = PAY_STATUS[p.statusPagamento];
      const chip = { waiting: [icon('calendar', 16), 'Aguardando confirmação'], pending: [icon('warning', 16), 'Pendente'], paid: [icon('check', 16), 'Pago'] }[st];
      return `<article class="payment-card">
        <div class="payment-student">
          <span class="initials">${esc(initials(p.aluno.nome))}</span>
          <div><h2>${esc(p.aluno.nome)}</h2><p>${esc(modalidade(p.aluno.modalidade))}${p.aluno.status === 'INATIVO' ? ' · Inativo' : ''}</p></div>
        </div>
        <div class="payment-value"><span>${ref}</span><strong>${money(valorPagamento(p))}</strong></div>
        <span class="payment-chip ${st}">${chip[0]}${chip[1]}</span>
        ${st === 'paid' ? `<p class="confirmed-at">Confirmado em ${esc(formatDateTime(p.dataHoraConfirmacao))}</p>` : ''}
        ${st === 'waiting' ? `<button class="button primary payment-action" data-action="confirm" data-id="${p.id}">${icon('check')} Confirmar pagamento</button>` : ''}
        ${st === 'pending' ? `<button class="button outline payment-action" data-action="receipt" data-id="${p.id}">${icon('receipt')} Registrar comprovante</button>` : ''}
        ${st === 'paid' ? `<button class="button undo payment-action" data-action="undo" data-id="${p.id}">${icon('undo')} Desfazer confirmação</button>` : ''}
      </article>`;
    }).join('');
  }

  function financePage() {
    const pays = pagamentosVisiveis();
    const count = (s) => pays.filter((p) => PAY_STATUS[p.statusPagamento] === s).length;
    const previsto = pays.length
      ? pays.reduce((s, p) => s + valorPagamento(p), 0)
      : ativos().reduce((s, a) => s + Number(a.valorMensalidade || 0), 0);
    const filters = [['all', 'Todos', pays.length], ['pending', 'Pendentes', count('pending')],
    ['waiting', 'Aguardando', count('waiting')], ['paid', 'Pagos', count('paid')]];

    return `<main class="page">
      ${pageIntro('PAINEL ADMINISTRATIVO', 'Financeiro', 'Acompanhe e confira as mensalidades do mês.')}
      ${monthPicker()}
      <section class="finance-summary">
        <div>${icon('students')}<strong>${ativos().length} alunos ativos</strong></div>
        <p>Mensalidades previstas <b class="nowrap">${money(previsto)}</b></p>
      </section>
      <label class="field">
        <span class="input-wrap">${icon('search', 19)}
          <input id="finance-search" aria-label="Buscar aluno no financeiro" placeholder="Buscar aluno pelo nome" value="${esc(state.financeQuery)}">
        </span>
      </label>
      <div class="finance-filters">
        ${filters.map(([k, label, n]) => `<button class="${state.financeFilter === k ? 'selected' : ''}" data-action="finance-filter" data-filter="${k}"><span>${label}</span><b>${n}</b></button>`).join('')}
      </div>
      <section class="payment-list" id="payment-list">${paymentCards()}</section>
      <p class="info-note">${icon('info', 18)} Recebeu o comprovante? Registre o recebimento e confirme após conferir.</p>
    </main>`;
  }

  /* ---------- Modais ---------- */

  function modalHtml() {
    const m = state.modal;
    if (!m) return '';
    let title; let label; let danger = false; let requireCheck = false; let content;

    if (m.type === 'confirm') {
      title = 'Confirmar pagamento'; label = 'Confirmar pagamento'; requireCheck = true;
      content = `<p>Você está confirmando a mensalidade de <strong>${esc(m.pagamento.aluno.nome)}</strong>.</p>
        <div class="modal-summary"><span>${capital(MESES[m.pagamento.mesReferencia - 1])} de ${m.pagamento.anoReferencia}</span><strong>${money(valorPagamento(m.pagamento))}</strong></div>
        <p>Confirme somente após conferir o comprovante e os dados do pagamento.</p>`;
    } else if (m.type === 'undo') {
      title = 'Desfazer confirmação?'; label = 'Desfazer confirmação'; danger = true;
      content = `<p>O pagamento de <strong>${esc(m.pagamento.aluno.nome)}</strong> voltará para “Aguardando confirmação”.</p>`;
    } else {
      title = 'Excluir aluno?'; label = 'Excluir aluno'; danger = true;
      content = `<p>O cadastro de <strong>${esc(m.aluno.nome)}</strong> será removido.</p>
        <p>Alunos com comprovante registrado ou pagamento confirmado não podem ser excluídos — nesse caso, altere o status para Inativo.</p>
        <p>Esta ação não pode ser desfeita.</p>`;
    }

    return `<div class="modal-layer" role="presentation">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <button class="modal-close" data-action="modal-cancel" aria-label="Fechar diálogo">${icon('x')}</button>
        <span class="modal-icon ${danger ? 'danger' : ''}">${icon(danger ? 'warning' : 'check', 25)}</span>
        <h2 id="modal-title">${title}</h2>
        <div class="modal-content">${content}</div>
        ${requireCheck ? `<label class="check-row"><input type="checkbox" id="modal-check"><span>${icon('check', 15)}</span><b>Conferi o comprovante e os dados do pagamento.</b></label>` : ''}
        <div class="modal-actions">
          <button class="button ${danger ? 'danger-button' : 'primary'}" data-action="modal-ok" ${requireCheck ? 'disabled' : ''}>${label}</button>
          <button class="button" data-action="modal-cancel">Cancelar</button>
        </div>
      </div>
    </div>`;
  }

  /* ---------- Renderização ---------- */

  function render() {
    const r = route();
    let page;
    if (r.screen === 'students') page = studentsPage();
    else if (r.screen === 'student-form') page = studentFormPage(r.id);
    else if (r.screen === 'finance') page = financePage();
    else page = dashboardPage();

    root.innerHTML = `<div class="app-shell">${header()}${page}${bottomNav(r.screen)}${modalHtml()}</div>`;
  }

  /* ---------- Ações ---------- */

  async function run(action, successMessage) {
    if (state.busy) return;
    state.busy = true;
    try {
      await action();
      await reload();
      if (successMessage) toast(successMessage);
    } catch (err) {
      handleError(err);
    } finally {
      state.busy = false;
    }
  }

  function findPagamento(id) {
    return state.pagamentos.find((p) => p.id === Number(id));
  }

  async function modalOk() {
    if (state.busy) return;
    const m = state.modal;
    state.modal = null;
    if (m.type === 'confirm') {
      await run(() => api(`/api/pagamentos/${m.pagamento.id}/confirmar`, { method: 'PATCH' }), 'Pagamento confirmado com sucesso.');
    } else if (m.type === 'undo') {
      await run(() => api(`/api/pagamentos/${m.pagamento.id}/desfazer-confirmacao`, { method: 'PATCH' }), 'Confirmação desfeita.');
    } else {
      await run(() => api(`/api/alunos/${m.aluno.id}`, { method: 'DELETE' }), 'Aluno excluído com sucesso.');
    }
    render();
  }

  function setFieldError(form, name, message) {
    const field = form.querySelector(`[data-field="${name}"]`);
    if (!field) return;
    const text = field.querySelector('.error-text');
    field.classList.toggle('field-error', !!message);
    text.hidden = !message;
    text.textContent = message || '';
  }

  async function saveStudent(form) {
    const data = Object.fromEntries(new FormData(form).entries());
    const dados = {
      nome: data.nome.trim(),
      telefone: data.telefone.trim(),
      modalidade: data.modalidade || null,
      turmaHorario: data.turmaHorario.trim(),
      faixaAtual: data.faixaAtual.trim(),
      valorMensalidade: data.valorMensalidade ? Number(data.valorMensalidade) : null,
      status: data.status || 'ATIVO',
    };

    const errors = {
      nome: dados.nome.length < 3 ? 'Informe o nome completo.' : '',
      telefone: dados.telefone.replace(/\D/g, '').length < 10 ? 'Informe um telefone com DDD.' : '',
      modalidade: !dados.modalidade ? 'Selecione a modalidade.' : '',
      turmaHorario: !dados.turmaHorario ? 'Informe a turma e o horário.' : '',
      faixaAtual: !dados.faixaAtual ? 'Informe a faixa atual.' : '',
      valorMensalidade: !dados.valorMensalidade || dados.valorMensalidade <= 0 ? 'Informe uma mensalidade válida.' : '',
    };
    Object.entries(errors).forEach(([k, v]) => setFieldError(form, k, v));
    const first = Object.keys(errors).find((k) => errors[k]);
    if (first) {
      const input = form.elements[first];
      if (input) input.focus();
      return;
    }

    const id = form.dataset.id;
    const submit = form.querySelector('[type="submit"]');
    submit.disabled = true;
    try {
      if (id) await api(`/api/alunos/${id}`, { method: 'PUT', body: dados });
      else await api('/api/alunos', { method: 'POST', body: dados });
      await loadAlunos();
      await loadPagamentos();
      go('/alunos');
      toast(id ? 'Dados do aluno atualizados.' : 'Aluno cadastrado com sucesso.');
    } catch (err) {
      submit.disabled = false;
      if (err.body && err.body.fields) {
        Object.entries(err.body.fields).forEach(([k, v]) => setFieldError(form, k, v));
      }
      handleError(err);
    }
  }

  root.addEventListener('click', async (event) => {
    const goEl = event.target.closest('[data-go]');
    if (goEl) {
      event.preventDefault();
      state.menuOpen = false;
      go(goEl.dataset.go);
      return;
    }
    const el = event.target.closest('[data-action]');
    if (!el) return;
    const { action, id } = el.dataset;

    switch (action) {
      case 'toggle-menu': state.menuOpen = !state.menuOpen; render(); break;
      case 'logout':
        try { await api('/api/auth/logout', { method: 'POST' }); } catch { /* sessão já expirada */ }
        location.replace('/login.html');
        break;
      case 'month-prev': changeMonth(-1); break;
      case 'month-next': changeMonth(1); break;
      case 'students-filter': state.studentsFilter = el.dataset.filter; render(); break;
      case 'finance-filter': state.financeFilter = el.dataset.filter; render(); break;
      case 'confirm': state.modal = { type: 'confirm', pagamento: findPagamento(id) }; render(); break;
      case 'undo': state.modal = { type: 'undo', pagamento: findPagamento(id) }; render(); break;
      case 'delete':
        state.modal = { type: 'delete', aluno: state.alunos.find((a) => a.id === Number(id)) };
        render();
        break;
      case 'receipt':
        run(() => api(`/api/pagamentos/${id}/comprovante`, { method: 'PATCH' }), 'Comprovante registrado. Aguarda conferência.');
        break;
      case 'generate': {
        const { mes, ano } = state.ref;
        run(() => api(`/api/pagamentos/gerar?mes=${mes}&ano=${ano}`, { method: 'POST' }), 'Mensalidades geradas.');
        break;
      }
      case 'modal-cancel': state.modal = null; render(); break;
      case 'modal-ok': modalOk(); break;
      default: break;
    }
  });

  root.addEventListener('change', (event) => {
    if (event.target.id === 'modal-check') {
      root.querySelector('[data-action="modal-ok"]').disabled = !event.target.checked;
    }
  });

  root.addEventListener('input', (event) => {
    const t = event.target;
    if (t.id === 'students-search') {
      state.studentsQuery = t.value;
      document.getElementById('student-list').innerHTML = studentCards();
    } else if (t.id === 'finance-search') {
      state.financeQuery = t.value;
      document.getElementById('payment-list').innerHTML = paymentCards();
    } else if (t.name === 'telefone') {
      t.value = formatPhone(t.value);
    }
  });

  root.addEventListener('submit', (event) => {
    if (event.target.id === 'student-form') {
      event.preventDefault();
      saveStudent(event.target);
    }
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && (state.modal || state.menuOpen)) {
      state.modal = null;
      state.menuOpen = false;
      render();
    }
  });

  window.addEventListener('hashchange', () => { state.menuOpen = false; render(); });

  /* ---------- Inicialização ---------- */

  (async function init() {
    try {
      state.user = await api('/api/auth/me');
      await loadAlunos();
      await loadPagamentos();
      render();
    } catch (err) {
      if (err && err.status === 401) location.replace('/login.html');
      else {
        root.innerHTML = `<div class="empty-state">${icon('warning', 28)}<h2>Não foi possível carregar o painel</h2><p>${esc(err.message)}</p></div>`;
      }
    }
  })();
})();
