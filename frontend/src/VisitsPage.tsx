import { useEffect, useState, FormEvent } from 'react';
import { api, errorMessage, Lane, Page, priorities, statuses, Visit } from './api';
import { ConfirmationDialog, Empty, ErrorNotice, Field, Loading, Pagination } from './components';
import { VisitEditor } from './VisitEditor';

export function VisitsPage({ editable, admin }: { editable: boolean; admin: boolean }) {
  const [filters, setFilters] = useState<Record<string, string>>({ archived: 'false' });
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Visit> | null>(null);
  const [lanes, setLanes] = useState<Lane[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [revision, setRevision] = useState(0);
  const [selected, setSelected] = useState<string[]>([]);
  const [editor, setEditor] = useState<Visit | 'new' | null>(null);
  const [message, setMessage] = useState('');
  const [filtersOpen, setFiltersOpen] = useState(window.innerWidth > 700);
  const [confirmation, setConfirmation] = useState<{ item: Visit; action: string } | null>(null);
  const params = new URLSearchParams({ ...filters, page: String(page), size: '20' });

  useEffect(() => {
    const media = window.matchMedia('(max-width: 700px)');
    const update = () => setFiltersOpen(!media.matches);
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, []);

  useEffect(() => {
    api<Lane[]>('/lanes').then(setLanes).catch(exception => setError(errorMessage(exception)));
  }, []);
  useEffect(() => {
    let active = true;
    setBusy(true);
    setError('');
    api<Page<Visit>>('/visits?' + params).then(result => {
      if (active) { setData(result); setSelected([]); }
    }).catch(exception => { if (active) setError(errorMessage(exception)); })
      .finally(() => { if (active) setBusy(false); });
    return () => { active = false; };
  }, [page, JSON.stringify(filters), revision]);

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const next: Record<string, string> = {};
    new FormData(event.currentTarget).forEach((value, key) => { if (String(value)) next[key] = String(value); });
    setFilters(next);
    setPage(0);
  }

  async function exportCsv() {
    try {
      const text = await api<string>('/visits/export?' + new URLSearchParams(filters));
      const url = URL.createObjectURL(new Blob([text], { type: 'text/csv;charset=utf-8' }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'pool-visits.csv';
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (exception) { setError(errorMessage(exception)); }
  }

  async function bulk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    try {
      const results = await api<{ success: boolean; error: string | null }[]>('/visits/bulk', 'POST', {
        ids: selected, action: form.get('action'), tag: form.get('tag'), status: form.get('status'),
      });
      setMessage('Обработано ' + results.filter(row => row.success).length + ' из ' + results.length + '. '
        + results.filter(row => !row.success).map(row => row.error).join(' '));
      setRevision(value => value + 1);
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  async function confirm() {
    if (!confirmation) return;
    setBusy(true);
    try {
      const { item, action } = confirmation;
      const suffix = action === 'archive' ? '' : '/' + action;
      await api('/visits/' + item.id + suffix, action === 'archive' || action === 'permanent' ? 'DELETE' : 'POST');
      setConfirmation(null);
      setRevision(value => value + 1);
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  if (editor) return <VisitEditor visit={editor === 'new' ? null : editor} editable={editable} lanes={lanes}
    onBack={() => setEditor(null)} onSaved={() => { setEditor(null); setRevision(value => value + 1); }} />;

  return <section>
    <div className="page-heading"><div><h1 className="heading-4">Посещения</h1>
      <p className="body-6">Расписание, записи клиентов и история сеансов</p></div>
      <div className="actions">
        <button onClick={exportCsv}>Экспорт CSV</button>
        <button onClick={() => setRevision(value => value + 1)}>Обновить</button>
        {editable && <button className="primary" onClick={() => setEditor('new')}>Записать клиента</button>}
      </div>
    </div>
    <details className="filter-panel" open={filtersOpen}
      onToggle={event => setFiltersOpen(event.currentTarget.open)}>
    <summary className="title-3">Поиск и фильтры</summary>
    <form className="filters" onSubmit={applyFilters}>
      <Field label="Поиск"><input name="query" placeholder="Клиент, телефон, номер" /></Field>
      <Field label="Дорожка"><select name="lane"><option value="">Все дорожки</option>{lanes.map(lane => <option key={lane.number} value={lane.number}>{lane.name}</option>)}</select></Field>
      <Field label="Статус"><select name="status"><option value="">Все статусы</option>{Object.entries(statuses).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
      <Field label="Записи"><select name="archived" defaultValue="false"><option value="false">Действующие</option><option value="true">Архивные</option><option value="">Все</option></select></Field>
      <Field label="С даты"><input type="date" name="from" /></Field>
      <Field label="По дату"><input type="date" name="to" /></Field>
      <Field label="Занятие"><select name="individual"><option value="">Любое</option><option value="true">Индивидуальное</option><option value="false">Обычное</option></select></Field>
      <Field label="Приоритет"><select name="priority"><option value="">Любой</option>{Object.entries(priorities).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></Field>
      <Field label="Тег"><input name="tag" maxLength={50} /></Field>
      <Field label="Длительность, мин"><input name="duration" type="number" min="0" /></Field>
      <Field label="Сравнение"><select name="comparison"><option value="EQ">Равно</option><option value="GT">Больше</option><option value="GTE">Не меньше</option><option value="LT">Меньше</option><option value="LTE">Не больше</option></select></Field>
      <Field label="Сортировка"><select name="sort"><option value="visitDate">По дате</option><option value="durationMinutes">По длительности</option><option value="visitNumber">По номеру</option></select></Field>
      <Field label="Порядок"><select name="descending"><option value="false">По возрастанию</option><option value="true">По убыванию</option></select></Field>
      <div className="actions"><button className="primary">Применить</button><button type="reset" onClick={() => { setFilters({ archived: 'false' }); setPage(0); }}>Сбросить</button></div>
    </form>
    </details>
    <ErrorNotice message={error} />
    {message && <p className="notice" role="status">{message}</p>}
    {editable && selected.length > 0 && <form className="bulk-bar" onSubmit={bulk}>
      <span className="title-4">Выбрано {selected.length}</span>
      <select name="action" aria-label="Массовое действие">
        <option value="ARCHIVE">Архивировать</option><option value="RESTORE">Восстановить</option>
        <option value="STATUS">Сменить статус</option><option value="ADD_TAG">Добавить тег</option><option value="REMOVE_TAG">Удалить тег</option>
      </select>
      <select name="status" aria-label="Новый статус"><option value="CANCELLED">Отменено</option><option value="PLANNED">Запланировано</option></select>
      <input name="tag" placeholder="Тег для операции" aria-label="Тег для операции" maxLength={50} />
      <button disabled={busy}>Выполнить</button>
    </form>}
    {busy ? <Loading /> : data?.items.length ? <div className="table-wrap"><table>
      <thead><tr>{editable && <th><input type="checkbox" aria-label="Выбрать все записи страницы"
        checked={selected.length === data.items.length} onChange={event => setSelected(event.target.checked ? data.items.map(item => item.id) : [])} /></th>}
        <th>Клиент и запись</th><th>Дата и время</th><th>Дорожка</th><th>Статус</th><th>Действия</th></tr></thead>
      <tbody>{data.items.map(item => <tr key={item.id}>
        {editable && <td><input type="checkbox" aria-label={'Выбрать ' + item.visitNumber} checked={selected.includes(item.id)}
          onChange={event => setSelected(event.target.checked ? [...selected, item.id] : selected.filter(id => id !== item.id))} /></td>}
        <td><button className="text-button title-3" onClick={() => setEditor(item)}>{item.clientName}</button>
          <span className="caption-4 muted">{item.visitNumber} · {item.title}</span>
          <span className="caption-4">{item.tags.join(' · ')}</span></td>
        <td>{new Date(item.visitDate + 'T00:00').toLocaleDateString('ru-RU')}<span className="caption-4 muted">{item.startTime.slice(0, 5)} · {item.durationMinutes} мин</span></td>
        <td>{item.laneName}<span className="caption-4 muted">{priorities[item.priority]}</span></td>
        <td><span className={'status ' + item.status.toLowerCase()}>{statuses[item.status]}</span>{item.archived && <span className="caption-4">В архиве</span>}</td>
        <td><div className="row-actions"><button onClick={() => setEditor(item)}>Открыть</button>
          {editable && <button onClick={() => setConfirmation({ item, action: item.archived ? 'restore' : 'archive' })}>{item.archived ? 'Восстановить' : 'В архив'}</button>}
          {editable && !item.archived && item.status === 'PLANNED' && <button onClick={() => setConfirmation({ item, action: 'cancel' })}>Отменить</button>}
          {admin && item.archived && <button className="danger" onClick={() => setConfirmation({ item, action: 'permanent' })}>Удалить</button>}</div></td>
      </tr>)}</tbody>
    </table></div> : <Empty>Измените условия поиска{editable ? ' или запишите клиента на свободный сеанс.' : ', чтобы найти посещения.'}</Empty>}
    {data && <div className="list-footer"><span className="caption-2">Найдено {data.totalItems} посещений</span><Pagination page={page} total={data.totalPages} onChange={setPage} /></div>}
    {confirmation && <ConfirmationDialog onClose={() => setConfirmation(null)}>
      <h2 id="confirm-title" className="heading-5">Подтвердите действие</h2>
      <p>{confirmation.action === 'permanent' ? 'Окончательно удалить запись ' : 'Изменить запись '}{confirmation.item.visitNumber} для клиента {confirmation.item.clientName}?</p>
      {confirmation.action === 'archive' && <p className="body-6">Архивирование сохраняет занятость времени. Для освобождения интервала отмените посещение.</p>}
      <div className="actions"><button autoFocus onClick={() => setConfirmation(null)}>Назад</button><button className="primary" disabled={busy} onClick={confirm}>Подтвердить</button></div>
    </ConfirmationDialog>}
  </section>;
}
