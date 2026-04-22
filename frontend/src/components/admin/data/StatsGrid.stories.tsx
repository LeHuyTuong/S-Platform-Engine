import type { Meta, StoryObj } from '@storybook/react-vite';
import { StatsGrid } from './StatsGrid';

const meta: Meta<typeof StatsGrid> = {
  title: 'Admin/Data/LướiThốngKê',
  component: StatsGrid,
  parameters: {
    layout: 'padded',
  },
  args: {
    stats: {
      totalJobs: 128,
      completedJobs: 93,
      failedJobs: 7,
      userCount: 24,
      diskUsageMb: 512.4,
      isYtDlpInstalled: true,
      isFfmpegInstalled: true,
    },
  },
};

export default meta;
type Story = StoryObj<typeof StatsGrid>;

export const MặcĐịnh: Story = {};
