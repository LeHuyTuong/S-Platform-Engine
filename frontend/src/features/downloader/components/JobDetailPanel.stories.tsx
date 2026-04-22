import type { Meta, StoryObj } from '@storybook/react-vite';
import type { Job } from '../../../api/downloaderTypes';
import { ApiClientError } from '../../../api/types';
import { JobDetailPanel } from './JobDetailPanel';

const runningJob: Job = {
  id: 'job_running',
  sourceRequestId: 'src_running',
  status: 'RUNNING',
  state: 'RUNNING',
  platform: 'YOUTUBE',
  sourceType: 'DIRECT_URL',
  url: 'https://www.youtube.com/watch?v=running',
  videoTitle: 'Chi tiết job đang chạy',
  playlistTitle: null,
  totalItems: 1,
  currentItem: 1,
  downloadType: 'VIDEO',
  quality: 'best',
  format: 'mp4',
  errorMessage: null,
  downloadSpeed: '2.8 MiB/s',
  eta: '00:12',
  progressPercent: 48,
  createdAt: '2026-04-22T12:00:00',
  logs: [
    '[download] Destination: running.mp4',
    '[download] 48.0% of 50.23MiB at 2.8MiB/s ETA 00:12',
  ],
};

const completedJob: Job = {
  ...runningJob,
  id: 'job_completed',
  status: 'COMPLETED',
  state: 'COMPLETED',
  progressPercent: 100,
  downloadSpeed: null,
  eta: null,
  logs: [
    '[download] Destination: completed.mp4',
    '[download] 100% of 50.23MiB in 00:00:45',
    'Finished downloading completed.mp4',
  ],
};

const failedJob: Job = {
  ...runningJob,
  id: 'job_failed',
  status: 'FAILED',
  state: 'FAILED',
  progressPercent: 12,
  errorMessage: 'ERROR: Sign in to confirm you are not a bot.',
  logs: [
    '[download] Destination: failed.mp4',
    'ERROR: Sign in to confirm you are not a bot.',
  ],
};

const meta: Meta<typeof JobDetailPanel> = {
  title: 'Downloader/ChiTiếtJob',
  component: JobDetailPanel,
  parameters: {
    layout: 'fullscreen',
  },
  render: (args) => (
    <div className="min-h-screen bg-bg p-6 text-text">
      <div className="mx-auto max-w-5xl">
        <JobDetailPanel {...args} />
      </div>
    </div>
  ),
};

export default meta;
type Story = StoryObj<typeof JobDetailPanel>;

export const ChưaChọnJob: Story = {
  args: {
    job: null,
    files: [],
    loading: false,
    refreshing: false,
    error: null,
    filesLoading: false,
    filesError: null,
    onRetry: () => undefined,
    onRetryFiles: () => undefined,
  },
};

export const ĐangChạy: Story = {
  args: {
    job: runningJob,
    files: [],
    loading: false,
    refreshing: true,
    error: null,
    filesLoading: false,
    filesError: null,
    onRetry: () => undefined,
    onRetryFiles: () => undefined,
  },
};

export const HoànTấtCóFile: Story = {
  args: {
    job: completedJob,
    files: [
      {
        name: 'completed.mp4',
        path: null,
        downloadUrl: '/api/v1/jobs/job_completed/files/completed.mp4',
        contentType: 'video/mp4',
        type: 'video',
        size: 52_428_800,
      },
    ],
    loading: false,
    refreshing: false,
    error: null,
    filesLoading: false,
    filesError: null,
    onRetry: () => undefined,
    onRetryFiles: () => undefined,
  },
};

export const ThấtBại: Story = {
  args: {
    job: failedJob,
    files: [],
    loading: false,
    refreshing: false,
    error: new ApiClientError({
      status: 500,
      code: 'INTERNAL_ERROR',
      message: 'Lần polling gần nhất thất bại nhưng dữ liệu job cũ vẫn còn trong state.',
    }),
    filesLoading: false,
    filesError: null,
    onRetry: () => undefined,
    onRetryFiles: () => undefined,
  },
};
