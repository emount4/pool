import { useEffect, useState } from 'react';
import { api, errorMessage, priorities, statuses } from './api';
import { ErrorNotice, Field, Loading } from './components';

interface Group { label: string; count: number }
interface Summary {
  totals: { total: number; active: number; archived: number; individual: number; averageDuration: number };
  lanes: Group[];
  statuses: Group[];
  priorities: Group[];
}

export function AnalyticsPage() {
  const [data, setData] = useState<Summary | null>(null);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [period, setPeriod] = useState('');
  useEffect(() => {
    setData(null);
    setError('');
    api<Summary>('/analytics/summary?' + period).then(setData).catch(exception => setError(errorMessage(exception)));
  }, [revision, period]);
  return <section>
    <div className="page-heading"><div><h1 className="heading-4">Аналитика</h1><p className="body-6">Сводка по датам посещений, включая архив</p></div>
      <button onClick={() => setRevision(value => value + 1)}>Обновить</button></div>
    <ErrorNotice message={error} />
    <form className="inline-form" onSubmit={event => {
      event.preventDefault();
      const params = new URLSearchParams();
      new FormData(event.currentTarget).forEach((value, key) => { if (value) params.set(key, String(value)); });
      setPeriod(params.toString());
    }}>
      <Field label="С даты"><input type="date" name="from" /></Field>
      <Field label="По дату"><input type="date" name="to" /></Field>
      <button>Применить период</button><button type="reset" onClick={() => setPeriod('')}>Все даты</button>
    </form>
    {!data ? (error ? null : <Loading />) : <>
      <dl className="totals">
        <div><dt>Всего посещений</dt><dd>{data.totals.total}</dd></div>
        <div><dt>Действующих записей</dt><dd>{data.totals.active}</dd></div>
        <div><dt>В архиве</dt><dd>{data.totals.archived}</dd></div>
        <div><dt>Индивидуальных</dt><dd>{data.totals.individual}</dd></div>
        <div><dt>Средняя длительность</dt><dd>{data.totals.averageDuration.toLocaleString('ru-RU', { maximumFractionDigits: 1 })}<small> мин</small></dd></div>
      </dl>
      <div className="analytics-grid"><GroupChart title="По дорожкам" groups={data.lanes} /><GroupChart title="По статусам" groups={data.statuses.map(group => ({ ...group, label: statuses[group.label] }))} />
        <GroupChart title="По приоритету" groups={data.priorities.map(group => ({ ...group, label: priorities[group.label] }))} /></div>
    </>}
  </section>;
}

function GroupChart({ title, groups }: { title: string; groups: Group[] }) {
  const maximum = Math.max(...groups.map(group => group.count), 1);
  return <section className="surface"><h2 className="heading-5">{title}</h2>
    {!groups.length && <p>Посещения ещё не добавлены.</p>}
    {groups.map(group => <div className="chart-row" key={group.label}>
      <div className="chart-label"><span>{group.label}</span><strong>{group.count}</strong></div>
      <div className="bar-track"><div style={{ width: String(group.count / maximum * 100) + '%' }} /></div>
    </div>)}
  </section>;
}
