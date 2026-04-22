import type { ApiEnvelope, ApiResponse } from './types';
import { ApiClientError } from './types';

const DEFAULT_TIMEOUT_MS = 15_000;
const CSRF_ENDPOINT = '/api/v1/auth/csrf';

interface CsrfTokenPayload {
  token: string;
  headerName: string;
  parameterName: string;
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: BodyInit | FormData | object | null;
  timeoutMs?: number;
  csrf?: boolean;
}

let csrfTokenCache: CsrfTokenPayload | null = null;
let csrfTokenPromise: Promise<CsrfTokenPayload> | null = null;

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !(value instanceof FormData);
}

function createAbortSignal(timeoutMs: number, externalSignal?: AbortSignal | null) {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);

  const abortFromExternal = () => controller.abort();
  externalSignal?.addEventListener('abort', abortFromExternal);

  return {
    signal: controller.signal,
    cleanup: () => {
      window.clearTimeout(timeoutId);
      externalSignal?.removeEventListener('abort', abortFromExternal);
    },
  };
}

async function parseEnvelope<T>(response: Response): Promise<ApiEnvelope<T> | null> {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}

async function fetchCsrfToken(): Promise<CsrfTokenPayload> {
  if (csrfTokenCache) {
    return csrfTokenCache;
  }

  if (!csrfTokenPromise) {
    csrfTokenPromise = request<CsrfTokenPayload>(CSRF_ENDPOINT, {
      method: 'GET',
      csrf: false,
    })
      .then((response) => {
        csrfTokenCache = response.data;
        return response.data;
      })
      .finally(() => {
        csrfTokenPromise = null;
      });
  }

  return csrfTokenPromise;
}

function isMutatingMethod(method?: string) {
  const normalized = method?.toUpperCase() ?? 'GET';
  return normalized === 'POST' || normalized === 'PUT' || normalized === 'PATCH' || normalized === 'DELETE';
}

function buildHeaders(headers?: HeadersInit) {
  return new Headers(headers);
}

function buildBody(body: RequestOptions['body'], headers: Headers): BodyInit | undefined {
  if (body == null) {
    return undefined;
  }

  if (body instanceof FormData) {
    return body;
  }

  if (typeof body === 'string' || body instanceof Blob || body instanceof URLSearchParams) {
    return body;
  }

  if (isPlainObject(body)) {
    headers.set('Content-Type', 'application/json');
    return JSON.stringify(body);
  }

  return body as BodyInit;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
  const {
    method = 'GET',
    headers,
    body,
    credentials = 'include',
    timeoutMs = DEFAULT_TIMEOUT_MS,
    signal,
    csrf = true,
    ...rest
  } = options;

  const requestHeaders = buildHeaders(headers);

  if (csrf && isMutatingMethod(method)) {
    const csrfToken = await fetchCsrfToken();
    requestHeaders.set(csrfToken.headerName, csrfToken.token);
  }

  const { signal: requestSignal, cleanup } = createAbortSignal(timeoutMs, signal);

  try {
    const response = await fetch(path, {
      method,
      headers: requestHeaders,
      body: buildBody(body, requestHeaders),
      credentials,
      signal: requestSignal,
      ...rest,
    });

    const envelope = await parseEnvelope<T>(response);

    if (!response.ok || !envelope?.success) {
      throw new ApiClientError({
        status: response.status,
        code: envelope?.error?.code ?? 'REQUEST_FAILED',
        message: envelope?.error?.message ?? envelope?.message ?? 'Yêu cầu thất bại.',
        fieldErrors: envelope?.error?.fieldErrors ?? [],
        requestId: envelope?.requestId ?? null,
        path: envelope?.path ?? path,
      });
    }

    return {
      data: envelope.data,
      message: envelope.message,
      meta: envelope.meta ?? null,
      requestId: envelope.requestId ?? null,
      path: envelope.path ?? null,
      timestamp: envelope.timestamp ?? null,
    };
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }

    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new ApiClientError({
        status: 0,
        code: 'TIMEOUT',
        message: `Yêu cầu quá thời gian ${Math.round(timeoutMs / 1000)} giây.`,
      });
    }

    throw new ApiClientError({
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Không thể kết nối tới máy chủ.',
    });
  } finally {
    cleanup();
  }
}
