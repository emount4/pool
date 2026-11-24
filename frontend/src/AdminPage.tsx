import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage, Lane, Page, roleNames, User } from './api';
import { Empty, ErrorNotice, Field, Loading, Pagination } from './components';

export function AdminPage() {
  const [tab, setTab] = useState('users');
  return <section>
    <div className="page-heading"><div><h1 className="heading-4">Управление</h1><p className="body-6">Пользователи, дорожки и история изменений</p></div></div>
    <nav className="tabs" aria-label="Управление">{[['users', 'Пользователи'], ['lanes', 'Дорожки'], ['audit', 'Аудит']].map(([key, label]) =>
      <button key={key} aria-current={tab === key ? 'page' : undefined} onClick={() => setTab(key)}>{label}</button>)}</nav>
    <p className="body-6"><a href="/swagger-ui.html" target="_blank" rel="noreferrer">Открыть Swagger</a></p>
    {tab === 'users' && <Users />}
    {tab === 'lanes' && <Lanes />}
    {tab === 'audit' && <Audit />}
  </section>;
}

function Users() {
  const [data, setData] = useState<Page<User> | null>(null);
  const [page, setPage] = useState(0);
  const [editor, setEditor] = useState<User | 'new' | null>(null);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [temporary, setTemporary] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    api<Page<User>>('/admin/users?page=' + page).then(setData).catch(exception => setError(errorMessage(exception)));
  }, [page, revision]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const roles = form.getAll('roles').map(String);
    if (!roles.length) { setError('Выберите хотя бы одну роль.'); return; }
    setBusy(true);
    try {
      const body = { username: form.get('username'), displayName: form.get('displayName'), roles, active: form.get('active') === 'on' };
      if (editor === 'new') {
        const result = await api<{ temporaryPassword: string }>('/admin/users', 'POST', body);
        setTemporary(result.temporaryPassword);
      } else {
        await api('/admin/users/' + (editor as User).id, 'PUT', body);
      }
      setEditor(null);
      setRevision(value => value + 1);
      setError('');
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  async function reset(user: User) {
    setBusy(true);
    try {
      const result = await api<{ temporaryPassword: string }>('/admin/users/' + user.id + '/reset-password', 'POST');
      setTemporary(result.temporaryPassword);
      setRevision(value => value + 1);
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  const editing = editor && editor !== 'new' ? editor : null;
  return <div className="surface">
    <div className="page-heading"><h2 className="heading-5">Учётные записи</h2><button className="primary" onClick={() => setEditor('new')}>Добавить пользователя</button></div>
    <ErrorNotice message={error} />
    {temporary && <div className="notice" role="status"><p>Временный пароль: <strong className="secret">{temporary}</strong></p><p>Передайте его пользователю. После входа потребуется смена пароля.</p><button onClick={() => setTemporary('')}>Скрыть пароль</button></div>}
    {editor && <form className="user-form" onSubmit={save} key={editing?.id ?? 'new'}>
      <h3 className="heading-6">{editing ? 'Изменить доступ' : 'Новый пользователь'}</h3>
      <div className="form-grid">
        <Field label="Имя"><input name="displayName" defaultValue={editing?.displayName} required maxLength={100} /></Field>
        <Field label="Логин"><input name="username" defaultValue={editing?.username} required minLength={3} maxLength={50} /></Field>
      </div>
      <fieldset><legend className="title-4">Роли</legend>{Object.entries(roleNames).map(([value, label]) => <label className="check" key={value}><input type="checkbox" name="roles" value={value} defaultChecked={editing ? editing.roles.includes(value) : value === 'VIEWER'} />{label}</label>)}</fieldset>
      {editing && <label className="check"><input type="checkbox" name="active" defaultChecked={editing.active} />Доступ разрешён</label>}
      <div className="actions"><button type="button" onClick={() => setEditor(null)}>Отмена</button><button className="primary" disabled={busy}>Сохранить</button></div>
    </form>}
    {!data ? (error ? null : <Loading />) : <><div className="table-wrap"><table><thead><tr><th>Пользователь</th><th>Роли</th><th>Доступ</th><th>Действия</th></tr></thead>
      <tbody>{data.items.map(user => <tr key={user.id}><td>{user.displayName}<span className="caption-4 muted">{user.username}</span></td><td>{user.roles.map(role => roleNames[role]).join(', ')}</td><td>{user.active ? 'Разрешён' : 'Отключён'}</td>
        <td><div className="actions"><button onClick={() => setEditor(user)}>Изменить</button><button disabled={busy} onClick={() => reset(user)}>Сбросить пароль</button></div></td></tr>)}</tbody></table></div>
      <Pagination page={page} total={data.totalPages} onChange={setPage} /></>}
  </div>;
}

function Lanes() {
  const [lanes, setLanes] = useState<Lane[] | null>(null);
  const [editor, setEditor] = useState<Lane | 'new' | null>(null);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    api<Lane[]>('/lanes').then(setLanes).catch(exception => setError(errorMessage(exception)));
  }, [revision]);
  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    try {
      await api('/lanes' + (editor === 'new' ? '' : '/' + (editor as Lane).number), editor === 'new' ? 'POST' : 'PUT',
        { name: form.get('name'), active: form.get('active') === 'on' });
      setEditor(null);
      setRevision(value => value + 1);
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }
  const editing = editor && editor !== 'new' ? editor : null;
  return <div className="surface">
    <div className="page-heading"><h2 className="heading-5">Дорожки</h2><button className="primary" onClick={() => setEditor('new')}>Добавить дорожку</button></div>
    <ErrorNotice message={error} />
    {editor && <form onSubmit={save} key={editing?.number ?? 'new'}>
      <Field label="Название"><input name="name" required maxLength={100} defaultValue={editing?.name} /></Field>
      <label className="check"><input name="active" type="checkbox" defaultChecked={editing?.active ?? true} />Доступна для записи</label>
      <div className="actions"><button type="button" onClick={() => setEditor(null)}>Отмена</button><button className="primary" disabled={busy}>Сохранить</button></div>
    </form>}
    {!lanes ? (error ? null : <Loading />) : <div className="table-wrap"><table><thead><tr><th>Номер</th><th>Название</th><th>Состояние</th><th>Действие</th></tr></thead><tbody>
      {lanes.map(lane => <tr key={lane.number}><td>{lane.number}</td><td>{lane.name}</td><td>{lane.active ? 'Доступна' : 'Отключена'}</td><td><button onClick={() => setEditor(lane)}>Изменить</button></td></tr>)}
    </tbody></table></div>}
  </div>;
}

interface AuditRow { id: string; username: string; eventType: string; entityType: string; entityId: string; occurredAt: string; description: string; correlationId: string }

function Audit() {
  const [data, setData] = useState<Page<AuditRow> | null>(null);
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState('');
  const [ascending, setAscending] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    api<Page<AuditRow>>('/admin/audit?' + new URLSearchParams({ query, page: String(page), ascending: String(ascending) }))
      .then(setData).catch(exception => setError(errorMessage(exception)));
  }, [query, page, ascending]);
  return <div className="surface"><h2 className="heading-5">Журнал аудита</h2>
    <form className="inline-form" onSubmit={event => { event.preventDefault(); setQuery(String(new FormData(event.currentTarget).get('query') ?? '')); setPage(0); }}>
      <Field label="Поиск в аудите"><input name="query" placeholder="Сотрудник, событие или UUID" /></Field>
      <button>Найти</button>
      <button type="button" onClick={() => { setAscending(value => !value); setPage(0); }}>{ascending ? 'Сначала старые' : 'Сначала новые'}</button>
    </form>
    <ErrorNotice message={error} />
    {!data ? (error ? null : <Loading />) : !data.items.length ? <Empty>Измените запрос, чтобы найти события.</Empty> : <>
      <div className="table-wrap"><table><thead><tr><th>Время</th><th>Сотрудник</th><th>Событие</th><th>Запись</th></tr></thead><tbody>
        {data.items.map(row => <tr key={row.id}><td>{new Date(row.occurredAt).toLocaleString('ru-RU')}</td><td>{row.username ?? 'Система'}</td><td>{row.description}<span className="caption-4 muted">{row.eventType}</span></td><td className="uuid">{row.entityId}<span className="caption-5 muted">Запрос {row.correlationId}</span></td></tr>)}
      </tbody></table></div><Pagination page={page} total={data.totalPages} onChange={setPage} />
    </>}
  </div>;
}
