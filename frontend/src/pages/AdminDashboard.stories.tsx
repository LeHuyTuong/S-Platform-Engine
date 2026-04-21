import type { Meta, StoryObj } from '@storybook/react';
import AdminDashboard from './AdminDashboard';

const meta: Meta<typeof AdminDashboard> = {
  title: 'Pages/AdminDashboard',
  component: AdminDashboard,
  parameters: {
    layout: 'fullscreen',
  },
};

export default meta;
type Story = StoryObj<typeof AdminDashboard>;

export const Default: Story = {};
