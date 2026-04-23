import type { Meta, StoryObj } from '@storybook/react-vite';
import type { SourceRequest } from '../../../api/downloaderTypes';
import { ApiClientError } from '../../../api/types';
import { SourceRequestList } from './SourceRequestList';

const requestWithJobs: SourceRequest = {
  id: 'src_123',
  platform: 'YOUTUBE',
  sourceType: 'DIRECT_URL',
  state: 'RESOLVED',
  sourceUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
  resolvedCount: 1,
  errorMessage: null,
  blockedReason: null,
  createdAt: '2026-04-22T10:30:00',
  jobs: [
    {
      id: 'job_123',
      sourceRequestId: 'src_123',
      status: 'RUNNING',
      state: 'RUNNING',
      platform: 'YOUTUBE',
      sourceType: 'DIRECT_URL',
      url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      videoTitle: 'Job đang chạy minh họa',
      playlistTitle: null,
      totalItems: 1,
      currentItem: 1,
      downloadType: 'VIDEO',
      quality: 'best',
      format: 'mp4',
      errorMessage: null,
      downloadSpeed: '2.4 MiB/s',
      eta: '00:15',
      progressPercent: 54,
      createdAt: '2026-04-22T10:31:00',
      logs: [],
    },
  ],
};

const requestResolving: SourceRequest = {
  id: 'src_456',
  platform: 'INSTAGRAM',
  sourceType: 'PROFILE',
  state: 'RESOLVING',
  sourceUrl: 'https://www.instagram.com/example_profile/',
  resolvedCount: 0,
  errorMessage: null,
  blockedReason: null,
  createdAt: '2026-04-22T11:00:00',
  jobs: [],
};

const requestBlocked: SourceRequest = {
  id: 'src_789',
  platform: 'TIKTOK',
  sourceType: 'DIRECT_URL',
  state: 'BLOCKED',
  sourceUrl: 'https://www.tiktok.com/@example/video/123',
  resolvedCount: 0,
  errorMessage: null,
  blockedReason: 'Provider yêu cầu cookie hoặc đăng nhập để tiếp tục.',
  createdAt: '2026-04-22T11:15:00',
  jobs: [],
};

const meta: Meta<typeof SourceRequestList> = {
  title: 'Downloader/DanhSáchYêuCầuNguồn',
  component: SourceRequestList,
  parameters: {
    layout: 'fullscreen',
  },
  render: (args) => (
    <div className="min-h-screen bg-bg p-6 text-text">
      <div className="mx-auto max-w-4xl">
        <SourceRequestList {...args} />
      </div>
    </div>
  ),
};

export default meta;
type Story = StoryObj<typeof SourceRequestList>;

export const CóDữLiệu: Story = {
  args: {
    requests: [requestWithJobs, requestResolving],
    loading: false,
    refreshing: false,
    error: null,
    selectedJobId: 'job_123',
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
};

export const ĐangPhânGiảiVàBịChặn: Story = {
  args: {
    requests: [requestResolving, requestBlocked],
    loading: false,
    refreshing: true,
    error: null,
    selectedJobId: null,
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
};

export const Trống: Story = {
  args: {
    requests: [],
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
    requests: [],
    loading: false,
    refreshing: false,
    error: new ApiClientError({
      status: 500,
      code: 'INTERNAL_ERROR',
      message: 'Đã có lỗi hệ thống xảy ra. Vui lòng thử lại sau.',
    }),
    selectedJobId: null,
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
};

export const NhiêuDữLiệu: Story = {
  args: {
    requests: Array.from({ length: 15 }).map((_, i) => ({
      ...requestWithJobs,
      id: `src_many_${i}`,
      sourceUrl: `https://www.youtube.com/watch?v=video_${i}`,
      createdAt: new Date(Date.now() - i * 3600000).toISOString(),
      jobs: i % 2 === 0 ? requestWithJobs.jobs : [],
    })),
    loading: false,
    refreshing: false,
    error: null,
    selectedJobId: null,
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
};

export const KhôngTìmThấyKếtQuả: Story = {
  args: {
    requests: [requestWithJobs, requestResolving],
    loading: false,
    refreshing: false,
    error: null,
    selectedJobId: null,
    onRetry: () => undefined,
    onSelectJob: () => undefined,
  },
  render: (args) => {
    return (
      <div className="min-h-screen bg-bg p-6 text-text">
        <div className="mx-auto max-w-4xl">
          {/* Simulate search query by wrapping or just relying on internal state if we exposed it, 
              but since it's internal, we'll just show the component and user can type. */}
          <SourceRequestList {...args} />
          <p className="mt-4 text-xs text-muted italic">* Nhập "abc" vào ô tìm kiếm để thấy trạng thái trống.</p>
        </div>
      </div>
    );
  }
};
