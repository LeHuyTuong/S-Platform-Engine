import { request } from './apiClient';
import type { SourceRequest, SubmitSourceRequestPayload } from './downloaderTypes';

interface SourceRequestListParams {
  page?: number;
  size?: number;
}

export function listSourceRequests({ page = 0, size = 10 }: SourceRequestListParams = {}) {
  const search = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  return request<SourceRequest[]>(`/api/v1/source-requests?${search.toString()}`);
}

export function submitSourceRequest(payload: SubmitSourceRequestPayload) {
  return request<SourceRequest>('/api/v1/source-requests', {
    method: 'POST',
    body: payload,
  });
}
