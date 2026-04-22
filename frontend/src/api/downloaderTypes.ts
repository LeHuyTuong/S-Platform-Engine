export type Platform = 'YOUTUBE' | 'TIKTOK' | 'INSTAGRAM' | 'FACEBOOK';
export type PlatformOption = Platform | 'AUTO';

export type SourceType = 'DIRECT_URL' | 'PLAYLIST' | 'PROFILE';
export type SourceTypeOption = SourceType | 'AUTO';

export type DownloadType = 'VIDEO' | 'AUDIO';

export type SourceRequestState = 'ACCEPTED' | 'RESOLVING' | 'RESOLVED' | 'BLOCKED' | 'FAILED';

export type JobState =
  | 'ACCEPTED'
  | 'RESOLVING'
  | 'QUEUED'
  | 'RUNNING'
  | 'POST_PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'RETRY_WAIT'
  | 'BLOCKED';

export type JobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface AuthSession {
  authenticated: boolean;
  email?: string | null;
  role?: string | null;
  enabled?: boolean;
  dailyQuota?: number;
  jobsToday?: number;
  canUseProxy?: boolean;
  canManageRuntimeSettings?: boolean;
}

export interface Job {
  id: string;
  sourceRequestId?: string | null;
  status?: JobStatus | null;
  state: JobState;
  platform?: Platform | null;
  sourceType?: SourceType | null;
  url: string;
  videoTitle?: string | null;
  playlistTitle?: string | null;
  totalItems?: number | null;
  currentItem?: number | null;
  downloadType: DownloadType;
  quality?: string | null;
  format: string;
  errorMessage?: string | null;
  downloadSpeed?: string | null;
  eta?: string | null;
  progressPercent: number;
  createdAt: string;
  logs: string[];
}

export interface JobFile {
  name: string;
  path?: string | null;
  downloadUrl?: string | null;
  contentType?: string | null;
  type?: string | null;
  size: number;
}

export interface SourceRequest {
  id: string;
  platform?: Platform | null;
  sourceType?: SourceType | null;
  state: SourceRequestState;
  sourceUrl: string;
  resolvedCount?: number | null;
  errorMessage?: string | null;
  blockedReason?: string | null;
  createdAt: string;
  jobs: Job[];
}

export interface SubmitSourceRequestPayload {
  sourceUrl: string;
  platform: PlatformOption;
  sourceType: SourceTypeOption;
  downloadType: DownloadType;
  quality: string | null;
  format: string;
  writeThumbnail: boolean;
  cleanMetadata: boolean;
  startTime: string | null;
  endTime: string | null;
  proxy: string | null;
  titleTemplate: string | null;
  watermarkText: string | null;
}
