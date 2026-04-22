export interface ApiFieldError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

export interface ApiErrorPayload {
  code: string;
  message: string;
  fieldErrors: ApiFieldError[];
}

export interface ApiMeta {
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
  meta?: ApiMeta | null;
  error?: ApiErrorPayload | null;
  requestId?: string | null;
  path?: string | null;
  timestamp?: string | null;
}

export interface ApiResponse<T> {
  data: T;
  message: string;
  meta: ApiMeta | null;
  requestId: string | null;
  path: string | null;
  timestamp: string | null;
}

interface ApiClientErrorInit {
  status: number;
  code: string;
  message: string;
  fieldErrors?: ApiFieldError[];
  requestId?: string | null;
  path?: string | null;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: ApiFieldError[];
  readonly requestId: string | null;
  readonly path: string | null;

  constructor({ status, code, message, fieldErrors = [], requestId = null, path = null }: ApiClientErrorInit) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.requestId = requestId;
    this.path = path;
  }
}

export function toApiClientError(
  error: unknown,
  fallbackMessage = 'Không thể kết nối tới máy chủ.',
): ApiClientError {
  if (error instanceof ApiClientError) {
    return error;
  }

  if (error instanceof Error) {
    return new ApiClientError({
      status: 0,
      code: 'NETWORK_ERROR',
      message: error.message || fallbackMessage,
    });
  }

  return new ApiClientError({
    status: 0,
    code: 'NETWORK_ERROR',
    message: fallbackMessage,
  });
}
