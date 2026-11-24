import { FormEvent, useEffect, useState } from 'react';
import { api, errorMessage, Lane, Visit, statuses } from './api';
import { ErrorNotice, Field, Loading } from './components';

interface Client { id: number; firstName: string; lastName: string; phone: string }
interface Slot { startTime: string; endTime: string; laneNumbers: number[] }
interface Note { id: string; authorName: string; content: string; pinned: boolean; createdAt: string; version: number }

export function VisitEditor({ visit, lanes, editable, onBack, onSaved }: {
  visit: Visit | null; lanes: Lane[]; editable: boolean; onBack: () => void; onSaved: () => void;
}) {
  const [clients, setClients] = useState<Client[]>([]);
  const [clientId, setClientId] = useState(visit?.clientId ?? 0);
  const [date, setDate] = useState(visit?.visitDate ?? '');
  const [duration, setDuration] = useState(visit?.durationMinutes ?? 60);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [choice, setChoice] = useState(visit ? visit.startTime + '/' + visit.laneNumber : '');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [notes, setNotes] = useState<Note[]>([]);
  const [editingNote, setEditingNote] = useState<Note | null>(null);
  const canEdit = editable && (!visit || (!visit.archived && visit.status === 'PLANNED'));
  const [searched, setSearched] = useState(false);
  const [newClient, setNewClient] = useState(false);

  async function createClient(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setBusy(true);
    try {
      const client = await api<Client>('/clients', 'POST', Object.fromEntries(data.entries()));
      setClients(current => [...current, client]);
      setClientId(client.id);
      setNewClient(false);
      setChoice('');
      setError('');
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  useEffect(() => {
    api<Client[]>('/clients').then(setClients).catch(exception => setError(errorMessage(exception)));
    if (visit) loadNotes();
  }, [visit?.id]);

  async function loadNotes() {
    try { setNotes(await api<Note[]>('/visits/' + visit!.id + '/notes')); }
    catch (exception) { setError(errorMessage(exception)); }
  }

  async function searchClients(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    try {
      const query = String(new FormData(event.currentTarget).get('query') ?? '');
      setClients(await api<Client[]>('/clients?query=' + encodeURIComponent(query)));
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  async function findSlots() {
    if (!clientId || !date) { setError('Выберите клиента и дату.'); return; }
    setBusy(true);
    setError('');
    try {
      const query = new URLSearchParams({ date, duration: String(duration), clientId: String(clientId) });
      if (visit) query.set('except', visit.id);
      setSlots(await api<Slot[]>('/visits/available-slots?' + query));
      setSearched(true);
      setChoice('');
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!choice) { setError('Выберите свободное время и дорожку.'); return; }
    const data = new FormData(event.currentTarget);
    const [startTime, lane] = choice.split('/');
    setBusy(true);
    setError('');
    try {
      await api('/visits' + (visit ? '/' + visit.id : ''), visit ? 'PUT' : 'POST', {
        clientId, laneNumber: Number(lane), visitDate: date, startTime, durationMinutes: duration,
        title: data.get('title'), description: data.get('description'),
        individual: data.get('individual') === 'on', version: visit?.version,
      });
      onSaved();
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  async function saveNote(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    setBusy(true);
    const data = new FormData(form);
    try {
      await api('/visits/' + visit!.id + '/notes' + (editingNote ? '/' + editingNote.id : ''),
        editingNote ? 'PUT' : 'POST', { content: data.get('content'), pinned: data.get('pinned') === 'on',
          version: editingNote?.version });
      setEditingNote(null);
      form.reset();
      await loadNotes();
    } catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  async function deleteNote(note: Note) {
    setBusy(true);
    try { await api('/visits/' + visit!.id + '/notes/' + note.id, 'DELETE'); await loadNotes(); }
    catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  return <section>
    <button onClick={onBack}>Назад к посещениям</button>
    <div className="page-heading"><div><h1 className="heading-4">{visit ? visit.visitNumber : 'Записать клиента'}</h1>
      <p>{visit ? visit.clientName + ' · ' + statuses[visit.status] : 'Выберите клиента, дату и свободный интервал'}</p></div></div>
    <ErrorNotice message={error} />
    <div className="editor-grid">
      <section className="surface">
        {canEdit && <button onClick={() => setNewClient(value => !value)}>{newClient ? 'Закрыть форму клиента' : 'Добавить клиента'}</button>}
        {newClient && <form className="user-form" onSubmit={createClient}>
          <h2 className="heading-6">Новый клиент</h2>
          <div className="form-grid">
            <Field label="Имя"><input name="firstName" required minLength={2} maxLength={100} /></Field>
            <Field label="Фамилия"><input name="lastName" required minLength={2} maxLength={100} /></Field>
            <Field label="Телефон, только цифры"><input name="phone" required pattern="[0-9]{7,15}" inputMode="tel" /></Field>
            <Field label="Дата рождения"><input name="birthDate" type="date" required /></Field>
            <Field label="Email"><input name="email" type="email" maxLength={255} /></Field>
          </div>
          <button disabled={busy}>Сохранить клиента</button>
        </form>}
        {canEdit && <form className="inline-form" onSubmit={searchClients}>
          <Field label="Найти клиента"><input name="query" placeholder="Фамилия или телефон" /></Field>
          <button disabled={busy}>Найти</button>
        </form>}
        <form onSubmit={save}>
          <fieldset disabled={!canEdit || busy}>
            <div className="form-grid">
              <Field label="Клиент"><select required value={clientId || ''} onChange={event => { setClientId(Number(event.target.value)); setChoice(''); }}>
                <option value="">Выберите клиента</option>
                {visit && !clients.some(client => client.id === visit.clientId) && <option value={visit.clientId}>{visit.clientName}</option>}
                {clients.map(client => <option key={client.id} value={client.id}>{client.lastName} {client.firstName} · {client.phone}</option>)}
              </select></Field>
              <Field label="Дата"><input type="date" required value={date} onChange={event => { setDate(event.target.value); setChoice(''); }} /></Field>
              <Field label="Длительность, мин"><select value={duration} onChange={event => { setDuration(Number(event.target.value)); setChoice(''); }}>
                {[30, 60, 90, 120, 150, 180].map(value => <option key={value}>{value}</option>)}</select></Field>
              <Field label="Название"><input name="title" required maxLength={120} defaultValue={visit?.title ?? 'Посещение бассейна'} /></Field>
            </div>
            {canEdit && <button type="button" onClick={findSlots}>Найти свободное время</button>}
            {searched && slots.length === 0 && <p className="notice">Свободных интервалов нет. Выберите другую дату или длительность.</p>}
            <Field label="Время и дорожка"><select value={choice} required onChange={event => setChoice(event.target.value)}>
              <option value="">Выберите интервал</option>
              {!searched && visit && <option value={visit.startTime + '/' + visit.laneNumber}>{visit.startTime.slice(0, 5)} · {visit.laneName}</option>}
              {slots.flatMap(slot => slot.laneNumbers.map(lane => <option key={slot.startTime + '/' + lane} value={slot.startTime + '/' + lane}>
                {slot.startTime.slice(0, 5)} — {slot.endTime.slice(0, 5)} · {lanes.find(item => item.number === lane)?.name ?? lane}
              </option>))}
            </select></Field>
            <Field label="Описание"><textarea name="description" maxLength={1000} defaultValue={visit?.description} rows={3} /></Field>
            <label className="check"><input type="checkbox" name="individual" defaultChecked={visit?.individual} />Индивидуальное занятие</label>
            {canEdit && <button className="primary" disabled={busy}>{busy ? 'Сохранение…' : 'Сохранить посещение'}</button>}
          </fieldset>
        </form>
        {visit && <p className="caption-2">Версия {visit.version}. Теги: {visit.tags.join(', ') || 'не добавлены'}. Управление тегами доступно через массовые действия в журнале.</p>}
      </section>
      {visit && <aside className="surface">
        <h2 className="heading-5">Заметки</h2>
        {!notes.length && <p className="body-6">Здесь появятся заметки к посещению.</p>}
        {notes.map(note => <article className="note" key={note.id}>
          <div className="caption-4 muted">{note.pinned ? 'Закреплено · ' : ''}{note.authorName} · {new Date(note.createdAt).toLocaleString('ru-RU')}</div>
          <p className="note-content">{note.content}</p>
          {editable && <div className="actions"><button onClick={() => setEditingNote(note)}>Изменить</button><button disabled={busy} onClick={() => deleteNote(note)}>Удалить</button></div>}
        </article>)}
        {editable && <form key={editingNote?.id ?? 'new'} onSubmit={saveNote}>
          <Field label={editingNote ? 'Изменить заметку' : 'Новая заметка'}><textarea name="content" required maxLength={4000} defaultValue={editingNote?.content} rows={4} /></Field>
          <label className="check"><input type="checkbox" name="pinned" defaultChecked={editingNote?.pinned} />Закрепить</label>
          <div className="actions">{editingNote && <button type="button" onClick={() => setEditingNote(null)}>Отмена</button>}<button disabled={busy}>Сохранить заметку</button></div>
        </form>}
      </aside>}
    </div>
    {busy && <Loading />}
  </section>;
}
