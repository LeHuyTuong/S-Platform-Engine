import type { Meta, StoryObj } from '@storybook/react-vite';
import { JobStatusDashboard } from './JobStatusDashboard';
import type { Job } from './types';

const meta: Meta<typeof JobStatusDashboard> = {
  title: 'Component/BảngĐiềuKhiểnTrạngTháiJob',
  component: JobStatusDashboard,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof JobStatusDashboard>;

const mockBase: Job = {
  id: 'job-123456789',
  url: 'https://youtube.com/watch?v=example',
  videoTitle: 'Sơn Tùng M-TP - Making My Way | Official Music Video',
  downloadType: 'VIDEO',
  format: 'mp4',
  state: 'RUNNING',
  progressPercent: 45,
  downloadSpeed: '2.5 MiB/s',
  speedHistory: [1.2, 1.5, 2.0, 2.2, 2.5, 2.3, 2.1, 2.4, 2.6, 2.5],
  eta: '00:02:15',
  logs: [
    '[youtube] Extracting URL: https://youtube.com/watch?v=example',
    '[youtube] v=example: Downloading webpage',
    '[youtube] v=example: Downloading android player API JSON',
    '[info] v=example: Downloading 1 format(s): 137+140',
    '[download] Destination: Making My Way.mp4',
    '[download]  45.0% of  50.23MiB at  2.5MiB/s ETA 00:02:15',
  ],
};

export const ĐangChạy: Story = {
  args: {
    job: mockBase,
  },
};

export const HoànTất: Story = {
  args: {
    job: {
      ...mockBase,
      state: 'COMPLETED',
      progressPercent: 100,
      downloadSpeed: 'Hoàn thành',
      eta: '-',
      logs: [
        ...mockBase.logs,
        '[download] 100% of 50.23MiB in 00:00:45',
        'Finished downloading Making My Way.mp4',
        '[WATERMARK] Đang thêm watermark "@SP-Platform"...',
        '[WATERMARK] Đã hoàn thành thêm watermark.',
      ],
    },
  },
};

export const ThấtBại: Story = {
  args: {
    job: {
      ...mockBase,
      state: 'FAILED',
      errorMessage: 'ERROR: Sign in to confirm you are not a bot. This helps protect our community.',
      logs: [
        ...mockBase.logs,
        'ERROR: Sign in to confirm you are not a bot. This helps protect our community.',
      ],
    },
  },
};

export const DạngPlaylist: Story = {
  args: {
    job: {
      ...mockBase,
      playlistTitle: 'V-Pop Hits 2024',
      currentItem: 3,
      totalItems: 10,
      progressPercent: 75,
    },
  },
};

export const Trống: Story = {
  args: {
    job: null,
  },
};
