import { FormEvent, useState } from 'react';
import { api, errorMessage } from './api';
import { ErrorNotice, Field, Loading } from './components';

interface Row { row: number; success: boolean; error: string | null }
interface Preview { id: string; validRows: number; invalidRows: number; rows: Row[] }

export function ImportPage() {
  const [preview, setPreview] = useState<Preview | null>(null);
  const [results, setResults] = useState<Row[] | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function check(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError('');
    setResults(null);
    setPreview(null);
    try { setPreview(await api<Preview>('/visits/import/preview', 'POST', new FormData(event.currentTarget))); }
    catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  async function apply() {
    setBusy(true);
    try { setResults(await api<Row[]>('/visits/import/apply', 'POST', { previewId: preview!.id })); }
    catch (exception) { setError(errorMessage(exception)); }
    finally { setBusy(false); }
  }

  return <section>
    <div className="page-heading"><div><h1 className="heading-4">Импорт посещений</h1>
      <p className="body-6">Проверьте CSV перед добавлением записей</p></div></div>
    <div className="surface">
      <p>Используйте CSV из экспорта журнала. Сохраните названия и порядок столбцов. Лимит файла — 1 МБ и 500 записей.</p>
      <form className="inline-form" onSubmit={check}>
        <Field label="Файл CSV"><input type="file" name="file" accept=".csv,text/csv" required /></Field>
        <Field label="Режим"><select name="mode"><option value="NEW_ONLY">Только новые записи</option><option value="UPSERT">Создать и обновить по номеру</option></select></Field>
        <button className="primary" disabled={busy}>Проверить файл</button>
      </form>
      <ErrorNotice message={error} />
      {busy && <Loading />}
      {preview && <>
        <p role="status">Корректных строк {preview.validRows}. Строк с ошибками {preview.invalidRows}.</p>
        <p className="body-6">При применении загрузятся корректные строки. Проверка действует один час. Занятость времени и версии будут проверены повторно.</p>
        <button disabled={busy || !preview.validRows || !!results} className="primary" onClick={apply}>Применить корректные строки</button>
        <div className="table-wrap"><table><thead><tr><th>Строка</th><th>Результат</th></tr></thead>
          <tbody>{(results ?? preview.rows).map(row => <tr key={row.row}><td>{row.row}</td><td>{row.success ? results ? 'Применено' : 'Готова к импорту' : row.error}</td></tr>)}</tbody>
        </table></div>
      </>}
    </div>
  </section>;
}
