import { FormEvent, useEffect, useState } from 'react';
import { acceptSession, api, clearSession, errorMessage, refreshSession, roleNames, Tokens, User } from './api';
import { ErrorNotice, Field, Loading } from './components';
import { VisitsPage } from './VisitsPage';
import { AdminPage } from './AdminPage';
import { AnalyticsPage } from './AnalyticsPage';
import { ImportPage } from './ImportPage';

export function App() {
  const [user, setUser] = useState<User | null>(null);
  const [checking, setChecking] = useState(true);
  const [page, setPage] = useState('visits');
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    refreshSession().then(tokens => setUser(tokens.user)).catch(() => {}).finally(() => setChecking(false));
  }, []);

  async function logout() {
    try {
      await api('/auth/logout', 'POST');
      clearSession();
      setUser(null);
      setPage('visits');
    } catch (exception) { setError(errorMessage(exception)); }
  }

  if (checking) return <main className="login-shell"><Loading /></main>;
  if (!user) return <AuthPage onLogin={tokens => setUser(acceptSession(tokens))} />;

  const admin = user.roles.includes('ADMIN');
  const editable = admin || user.roles.includes('OPERATOR');
  if (user.passwordChangeRequired || passwordOpen) {
    return <PasswordForm required={user.passwordChangeRequired}
      onBack={() => setPasswordOpen(false)}
      onChanged={tokens => { setUser(acceptSession(tokens)); setPasswordOpen(false); }} />;
  }

  return <>
    <a className="skip-link" href="#content">К содержимому</a>
    <header className="app-header">
      <a href="/" className="brand"><img src="/favicon.svg" alt="" />Бассейн</a>
      <nav aria-label="Разделы">
        <button aria-current={page === 'visits' ? 'page' : undefined} onClick={() => setPage('visits')}>Посещения</button>
        <button aria-current={page === 'analytics' ? 'page' : undefined} onClick={() => setPage('analytics')}>Аналитика</button>
        {editable && <button aria-current={page === 'import' ? 'page' : undefined} onClick={() => setPage('import')}>Импорт CSV</button>}
        {admin && <button aria-current={page === 'admin' ? 'page' : undefined} onClick={() => setPage('admin')}>Управление</button>}
      </nav>
      <div className="account">
        <span className="title-4">{user.displayName}<small className="caption-4">{user.roles.map(role => roleNames[role]).join(', ')}</small></span>
        <button onClick={() => setPasswordOpen(true)}>Пароль</button>
        <button onClick={logout}>Выйти</button>
      </div>
    </header>
    <main id="content" className="workspace">
      <ErrorNotice message={error} />
      {page === 'visits' && <VisitsPage editable={editable} admin={admin} />}
      {page === 'analytics' && <AnalyticsPage />}
      {page === 'import' && <ImportPage />}
      {page === 'admin' && <AdminPage />}
    </main>
    <footer className="footer caption-5">Бассейн · Учёт посещений</footer>
  </>;
}

function AuthPage({ onLogin }: { onLogin: (tokens: Tokens) => void }) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [registering, setRegistering] = useState(false);

  function switchMode(registration: boolean) {
    setRegistering(registration);
    setError('');
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    if (registering && data.get('password') !== data.get('passwordConfirmation')) {
      setError('Подтверждение пароля не совпадает.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const credentials = {
        username: data.get('username'), password: data.get('password'),
      };
      const body = registering ? {
        ...credentials, displayName: data.get('displayName'),
        passwordConfirmation: data.get('passwordConfirmation'),
      } : credentials;
      onLogin(await api<Tokens>(registering ? '/auth/register' : '/auth/login', 'POST', body));
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }
  return <main className="login-shell">
    <div className="login-intro">
      <img src="/favicon.svg" width="48" height="48" alt="" />
      <h1 className="heading-2">Каждому посещению<br />своё время.</h1>
      <p className="body-3">Расписание дорожек, записи и история посещений в одном месте.</p>
      <div className="hours"><span className="title-1">07:00 — 22:00</span><span className="caption-2">Время работы бассейна</span></div>
    </div>
    <section className="login-form">
      <nav className="tabs" aria-label="Вход и регистрация">
        <button type="button" disabled={busy} aria-current={!registering ? 'page' : undefined}
          onClick={() => switchMode(false)}>Вход</button>
        <button type="button" disabled={busy} aria-current={registering ? 'page' : undefined}
          onClick={() => switchMode(true)}>Регистрация</button>
      </nav>
      <h2 className="heading-5">{registering ? 'Регистрация' : 'Вход'}</h2>
      <p className="body-6">{registering ? 'Создайте учётную запись для доступа к системе.' : 'Введите логин и пароль.'}</p>
      <form onSubmit={submit} key={registering ? 'register' : 'login'}>
        {registering && <Field label="Имя"><input name="displayName" required maxLength={100} autoComplete="name" /></Field>}
        <Field label="Логин"><input name="username" required minLength={registering ? 3 : undefined} maxLength={50} autoComplete="username" autoFocus={!registering} /></Field>
        <Field label="Пароль"><input name="password" required type="password" minLength={registering ? 8 : undefined} maxLength={72} autoComplete={registering ? 'new-password' : 'current-password'} /></Field>
        {registering && <>
          <p className="caption-2">Не менее 8 символов, включая букву и цифру.</p>
          <Field label="Повторите пароль"><input name="passwordConfirmation" required type="password" maxLength={72} autoComplete="new-password" /></Field>
          <p className="caption-2">Новая учётная запись получает роль наблюдателя. Администратор может изменить права.</p>
        </>}
        <ErrorNotice message={error} />
        <button className="primary" disabled={busy}>
          {busy ? 'Подождите…' : registering ? 'Зарегистрироваться' : 'Войти'}
        </button>
      </form>
    </section>
  </main>;
}

function PasswordForm({ required, onBack, onChanged }: {
  required: boolean; onBack: () => void; onChanged: (tokens: Tokens) => void;
}) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    if (data.get('newPassword') !== data.get('confirmation')) {
      setError('Подтверждение пароля не совпадает.');
      return;
    }
    setBusy(true);
    try {
      onChanged(await api<Tokens>('/auth/change-password', 'POST', {
        oldPassword: data.get('oldPassword'), newPassword: data.get('newPassword'),
      }));
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }
  return <main className="narrow">
    <h1 className="heading-4">Сменить пароль</h1>
    <p>{required ? 'Замените временный пароль, чтобы начать работу.' : 'После смены пароля остальные сессии завершатся.'}</p>
    <form onSubmit={submit}>
      <Field label="Текущий пароль"><input name="oldPassword" type="password" required autoComplete="current-password" /></Field>
      <Field label="Новый пароль"><input name="newPassword" type="password" required minLength={8} maxLength={72} autoComplete="new-password" /></Field>
      <p className="caption-2">Не менее 8 символов, включая букву и цифру.</p>
      <Field label="Повторите новый пароль"><input name="confirmation" type="password" required autoComplete="new-password" /></Field>
      <ErrorNotice message={error} />
      <div className="actions">
        {!required && <button type="button" onClick={onBack}>Назад</button>}
        <button className="primary" disabled={busy}>{busy ? 'Сохранение…' : 'Сохранить пароль'}</button>
      </div>
    </form>
  </main>;
}
