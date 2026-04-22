export type JobState =
  | 'ACCEPTED'
  | 'RESOLVING'
  | 'PENDING'
  | 'QUEUED'
  | 'RUNNING'
  | 'POST_PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'BLOCKED'
  | 'RETRY_WAIT';

export interface Job {
  id: string;
  url: string;
  videoTitle?: string | null;
  playlistTitle?: string | null;
  downloadType: 'VIDEO' | 'AUDIO';
  format: string;
  state: JobState;
  status?: string | null;
  progressPercent: number;
  downloadSpeed?: string | null;
  speedHistory?: Array<number | null>;
  eta?: string | null;
  currentItem?: number | null;
  totalItems?: number | null;
  errorMessage?: string | null;
  logs: string[];
}
