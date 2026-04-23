import type { Job, JobState, JobStatus, Platform } from './downloaderTypes';

export interface AdminDashboardStats {
  totalJobs: number;
  completedJobs: number;
  failedJobs: number;
  userCount: number;
  diskUsageMb: number;
  isYtDlpInstalled: boolean;
  isFfmpegInstalled: boolean;
}

export interface AdminJob extends Job {
  ownerEmail?: string | null;
  ownerRole?: string | null;
}

export interface AdminUser {
  id: number;
  email: string;
  role: string;
  enabled: boolean;
  createdAt?: string | null;
}

export interface AdminSettings {
  sleepInterval: number;
  concurrentFragments: number;
  sleepRequests: number;
  retries: number;
  maxFileSizeMb: number;
  telegramBotToken?: string;
  telegramChatId?: string;
  googleDriveServiceAccountJson?: string;
  googleDriveFolderId?: string;
  baseUrl?: string;
  diskUsageMb: number;
}

export interface AdminJobListParams {
  page?: number;
  size?: number;
  state?: JobState | '';
  status?: JobStatus | '';
  platform?: Platform | '';
}

export interface AdminResubmitResponse {
  jobId: string;
}

export interface AdminBackfillResponse {
  updated: number;
}
