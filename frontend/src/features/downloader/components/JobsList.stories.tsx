import type { Meta, StoryObj } from '@storybook/react-vite';
import type { Job } from '../../../api/downloaderTypes';
import { ApiClientError } from '../../../api/types';
import { JobsList } from './JobsList';

const sampleJobs: Job[] = [
  {
    id: 'job_001',
    sourceRequestId: 'src_001',
    status: 'RUNNING',
    state: 'RUNNING',
    platform: 'YOUTUBE',
    sourceType: 'DIRECT_URL',
    url: 'https://www.youtube.com/watch?v=one',
    videoTitle: 'Job đang chạy minh họa',
    playlistTitle: null,
    totalItems: 1,
    currentItem: 1,
    downloadType: 'VIDEO',
    quality: 'best',
    format: 'mp4',
    errorMessage: null,
    downloadSpeed: '3.1 MiB/s',
    eta: '00:09',
    progressPercent: 61,
    createdAt: '2026-04-22T11:30:00',
    logs: [],
  },
  {
    id: 'job_002',
    sourceRequestId: 'src_002',
    status: 'COMPLETED',
    state: 'COMPLETED',
    platform: 'INSTAGRAM',
    sourceType: 'PROFILE',
    url: 'https://www.instagram.com/reel/example',
    videoTitle: 'Job hoàn tất minh họa',
    playlistTitle: null,
    totalItems: 1,
    currentItem: 1,
    downloadType: 'AUDIO',
    quality: 'best',
    format: 'mp3',
    errorMessage: null,
    downloadSpeed: null,
    eta: null,
    progressPercent: 100,
    createdAt: '2026-04-22T10:15:00',
    logs: [],
  },
];

const meta: Meta<typeof JobsList> = {
  title: 'Downloader/DanhSáchJob',
  component: JobsList,
  parameters: {
    layout: 'fullscreen',
  },
  render: (args) => (
    <div className="min-h-screen bg-bg p-6 text-text">
      <div className="mx-auto max-w-5xl">
        <JobsList {...args} />
      </div>
    </div>
  ),
};

export default meta;
type Story = StoryObj<typeof JobsList>;

export const CóDữLiệu: Story = {
  args: {
    jobs: sampleJobs,
    meta: {
      page: 0,
      size: 20,
      totalItems: 2,
      totalPages: 1,
      hasNext: false,
      hasPrevious: false,
    },
    loading: false,
    refreshing: false,
    error: null,
    selectedJobId: 'job_001',
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
};

export const Trống: Story = {
  args: {
    jobs: [],
    meta: null,
    loading: false,
    refreshing: false,
    error: null,
    selectedJobId: null,
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
};

export const LỗiTảiDữLiệu: Story = {
  args: {
    jobs: [],
    meta: null,
    loading: false,
    refreshing: false,
    error: new ApiClientError({
      status: 401,
      code: 'UNAUTHORIZED',
      message: 'Phiên đăng nhập đã hết hạn.',
    }),
    selectedJobId: null,
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
};
