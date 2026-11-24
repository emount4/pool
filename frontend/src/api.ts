export interface User {
  id: string;
  username: string;
  displayName: string;
  roles: string[];
  active: boolean;
  passwordChangeRequired: boolean;
}

export interface Tokens {
  accessToken: string;
  user: User;
}

let accessToken = '';
let refreshing: Promise<Tokens> | null = null;

export function acceptSession(tokens: Tokens) {
  accessToken = tokens.accessToken;
  return tokens.user;
}

export function clearSession() {
  accessToken = '';
}

export async function refreshSession(): Promise<Tokens> {
  if (!refreshing) {
    refreshing = fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'X-Pool-Client': 'web' },
    }).then(readResponse<Tokens>).then(tokens => {
      acceptSession(tokens);
      return tokens;
    }).finally(() => { refreshing = null; });
  }
  return refreshing;
}

async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    const fields = Object.values(error.fields ?? {}).join(' ');
    throw new Error([error.message ?? 'Запрос не выполнен.', fields].filter(Boolean).join(' '));
  }
  if (response.status === 204) return undefined as T;
  if (response.headers.get('content-type')?.includes('text/csv')) return await response.text() as T;
  return response.json();
}

export async function api<T>(path: string, method = 'GET', body?: unknown, retry = true): Promise<T> {
  const headers: Record<string, string> = { 'X-Pool-Client': 'web' };
  if (accessToken && !['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'].includes(path)) {
    headers.Authorization = 'Bearer ' + accessToken;
  }
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
  const response = await fetch('/api' + path, {
    method,
    headers,
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  });
  if (response.status === 401 && retry && !path.startsWith('/auth/')) {
    await refreshSession();
    return api<T>(path, method, body, false);
  }
  return readResponse<T>(response);
}

export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'Не удалось выполнить действие.';
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Visit {
  id: string;
  visitNumber: string;
  clientId: number;
  clientName: string;
  laneNumber: number;
  laneName: string;
  visitDate: string;
  startTime: string;
  durationMinutes: number;
  status: string;
  title: string;
  description: string;
  individual: boolean;
  archived: boolean;
  priority: string;
  version: number;
  tags: string[];
}

export interface Lane {
  number: number;
  name: string;
  active: boolean;
}

export const statuses: Record<string, string> = {
  PLANNED: 'Запланировано',
  IN_PROGRESS: 'Идёт сейчас',
  COMPLETED: 'Завершено',
  CANCELLED: 'Отменено',
};

export const priorities: Record<string, string> = {
  STANDARD: 'Стандартный',
  ELEVATED: 'Повышенный',
  HIGH: 'Высокий',
};

export const roleNames: Record<string, string> = {
  VIEWER: 'Наблюдатель',
  OPERATOR: 'Оператор',
  ADMIN: 'Администратор',
};
