(function () {
  document.querySelectorAll('[data-icon]').forEach((el) => {
    el.outerHTML = icon(el.dataset.icon, el.dataset.icon === 'arrow' ? 20 : 19);
  });

  const form = document.getElementById('login-form');
  const toggle = document.getElementById('toggle-senha');
  const senhaInput = form.elements.senha;
  const authError = document.getElementById('auth-error');
  const button = document.getElementById('login-button');

  // Já está logado? Vai direto para o painel.
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

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const email = form.elements.email.value.trim();
    const senha = senhaInput.value;

    const emailError = !email ? 'Informe o e-mail.'
      : !/\S+@\S+\.\S+/.test(email) ? 'Informe um e-mail válido.' : '';
    const senhaError = !senha ? 'Informe a senha.' : '';
    setError('field-email', emailError);
    setError('field-senha', senhaError);
    showAuthError('');
    if (emailError || senhaError) return;

    button.disabled = true;
    try {
      await api('/api/auth/login', { method: 'POST', form: { email, senha } });
      location.replace('/');
    } catch (err) {
      showAuthError(err.status === 401 ? 'E-mail ou senha incorretos.'
        : 'Não foi possível entrar. Tente novamente.');
      button.disabled = false;
    }
  });
})();
