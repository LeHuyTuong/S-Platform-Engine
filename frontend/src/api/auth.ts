import { request } from './apiClient';
import type { AuthSession } from './downloaderTypes';

export function getAuthSession() {
  return request<AuthSession>('/api/v1/auth/me');
}

export function logout() {
  return request<void>('/api/v1/auth/logout', {
    method: 'POST',
  });
}
