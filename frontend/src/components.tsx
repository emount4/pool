import { ReactNode, useEffect, useRef } from 'react';

export function ConfirmationDialog({ children, onClose }: { children: ReactNode; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    dialog.current?.showModal();
    return () => dialog.current?.close();
  }, []);
  return <dialog ref={dialog} className="dialog" aria-labelledby="confirm-title"
    onCancel={onClose}>{children}</dialog>;
}

export function ErrorNotice({ message }: { message: string }) {
  return message ? <div className="notice error" role="alert">{message}</div> : null;
}

export function Loading() {
  return <div className="skeleton" role="status" aria-label="Загрузка данных">
    <div /><div /><div />
  </div>;
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return <label className="field"><span className="title-4">{label}</span>{children}</label>;
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty"><h3 className="heading-6">Пока нет записей</h3><p>{children}</p></div>;
}

export function Pagination({ page, total, onChange }: {
  page: number; total: number; onChange: (page: number) => void;
}) {
  return <div className="pagination">
    <button disabled={page === 0} onClick={() => onChange(page - 1)}>Назад</button>
    <span className="caption-2">Страница {page + 1} из {Math.max(1, total)}</span>
    <button disabled={page + 1 >= total} onClick={() => onChange(page + 1)}>Далее</button>
  </div>;
}
