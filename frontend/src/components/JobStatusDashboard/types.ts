export type JobState = 'ACCEPTED' | 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'BLOCKED' | 'QUEUED' | 'RETRY_WAIT';

export interface Job {
  id: string;
  url: string;
  videoTitle?: string;
  playlistTitle?: string;
  downloadType: 'VIDEO' | 'AUDIO';
  format: string;
  state: JobState;
  status?: string; // Legacy field
  progressPercent: number;
  downloadSpeed?: string;
  speedHistory?: (number | null)[];
  eta?: string;
  currentItem?: number;
  totalItems?: number;
  errorMessage?: string;
  logs: string[];
}
