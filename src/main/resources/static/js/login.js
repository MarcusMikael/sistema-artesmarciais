import { api, esc, icon } from './common.js';

(function () {
  const ICON_SIZES = { arrow: 20, check: 15 };
  document.querySelectorAll('[data-icon]').forEach((el) => {
    el.outerHTML = icon(el.dataset.icon, ICON_SIZES[el.dataset.icon] || 19);
  });

  const form = document.getElementById('login-form');
  const toggle = document.getElementById('toggle-senha');
  const senhaInput = form.elements.senha;
  const authError = document.getElementById('auth-error');
  const button = document.getElementById('login-button');
  const buttonText = button.querySelector('.login-button-text');
  const esqueci = document.getElementById('esqueci-senha');
  const ajuda = document.getElementById('senha-ajuda');

  // Já está logado (sessão ou "Lembrar de mim")? Vai direto para o painel.
  api('/api/auth/me').then(() => location.replace('/')).catch(() => {});

  function renderToggle() {
    const visible = senhaInput.type === 'text';
    toggle.innerHTML = icon(visible ? 'eyeOff' : 'eye', 19);
    toggle.setAttribute('aria-label', visible ? 'Ocultar senha' : 'Mostrar senha');
  }
  toggle.addEventListener('click', () => {
    senhaInput.type = senhaInput.type === 'password' ? 'text' : 'password';
    renderToggle();
  });
  renderToggle();

  // "Esqueceu a senha?" mostra como redefinir: a senha é configurada no servidor.
  esqueci.addEventListener('click', () => {
    const abrir = ajuda.hidden;
    ajuda.hidden = !abrir;
    esqueci.setAttribute('aria-expanded', String(abrir));
  });

  function setError(fieldId, message) {
    const field = document.getElementById(fieldId);
    const text = field.querySelector('.error-text');
    field.classList.toggle('field-error', !!message);
    text.hidden = !message;
    text.textContent = message || '';
  }

  function showAuthError(message) {
    authError.hidden = !message;
    authError.innerHTML = message ? icon('warning', 17) + esc(message) : '';
  }

  function setLoading(loading) {
    button.classList.toggle('is-loading', loading);
    button.disabled = loading;
    button.setAttribute('aria-busy', String(loading));
    buttonText.textContent = loading ? 'Entrando…' : 'Entrar no painel';
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (button.classList.contains('is-loading')) return;

    const email = form.elements.email.value.trim();
    const senha = senhaInput.value;

    const emailError = !email ? 'Informe o e-mail.'
      : !/\S+@\S+\.\S+/.test(email) ? 'Informe um e-mail válido.' : '';
    const senhaError = !senha ? 'Informe a senha.' : '';
    setError('field-email', emailError);
    setError('field-senha', senhaError);
    showAuthError('');
    if (emailError || senhaError) return;

    const dados = { email, senha };
    if (form.elements.lembrar.checked) dados.lembrar = 'on';

    setLoading(true);
    try {
      await api('/api/auth/login', { method: 'POST', form: dados });
      location.replace('/');
    } catch (err) {
      showAuthError(err.status === 401 ? 'E-mail ou senha incorretos.'
        : err.status === 429 ? err.message
          : 'Não foi possível entrar. Tente novamente.');
      setLoading(false);
    }
  });
})();
