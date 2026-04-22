import { request } from './apiClient';
import type { AdminBackfillResponse, AdminDashboardStats, AdminJob, AdminJobListParams, AdminResubmitResponse, AdminSettings, AdminUser } from './adminTypes';

export function getAdminDashboard() {
  return request<AdminDashboardStats>('/api/v1/admin/dashboard');
}

export function listAdminJobs({ page = 0, size = 20, state, status, platform }: AdminJobListParams = {}) {
  const search = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (state) {
    search.set('state', state);
  }

  if (status) {
    search.set('status', status);
  }

  if (platform) {
    search.set('platform', platform);
  }

  return request<AdminJob[]>(`/api/v1/admin/jobs?${search.toString()}`);
}

export function listAdminUsers() {
  return request<AdminUser[]>('/api/v1/admin/users');
}

export function getAdminSettings() {
  return request<AdminSettings>('/api/v1/admin/settings');
}

export function updateAdminSettings(settings: Omit<AdminSettings, 'diskUsageMb'>) {
  const payload = Object.fromEntries(
    Object.entries(settings).map(([key, value]) => [key, String(value)]),
  );

  return request<void>('/api/v1/admin/settings', {
    method: 'PUT',
    body: payload,
  });
}

export function resubmitAdminJob(jobId: string) {
  return request<AdminResubmitResponse>(`/api/v1/admin/jobs/${jobId}/resubmit`, {
    method: 'POST',
  });
}

export function backfillAdminJobTitles() {
  return request<AdminBackfillResponse>('/api/v1/admin/jobs/backfill-titles', {
    method: 'POST',
  });
}
