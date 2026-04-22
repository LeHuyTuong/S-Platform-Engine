import { request } from './apiClient';
import type { Job, JobFile, JobState, JobStatus, Platform } from './downloaderTypes';

interface JobListParams {
  page?: number;
  size?: number;
  state?: JobState;
  status?: JobStatus;
  platform?: Platform;
}

export function listJobs({ page = 0, size = 20, state, status, platform }: JobListParams = {}) {
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

  return request<Job[]>(`/api/v1/jobs?${search.toString()}`);
}

export function getJob(jobId: string) {
  return request<Job>(`/api/v1/jobs/${jobId}`);
}

export function listJobFiles(jobId: string) {
  return request<JobFile[]>(`/api/v1/jobs/${jobId}/files`);
}
