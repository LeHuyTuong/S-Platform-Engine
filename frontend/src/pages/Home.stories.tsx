import type { Meta, StoryObj } from '@storybook/react-vite';
import Home from './Home';

const meta: Meta<typeof Home> = {
  title: 'Trang/Home',
  component: Home,
  parameters: {
    layout: 'fullscreen',
  },
};

export default meta;
type Story = StoryObj<typeof Home>;

export const MànHìnhDesktop: Story = {};

export const MànHìnhMobile: Story = {
  parameters: {
    viewport: {
      defaultViewport: 'mobile1',
    },
  },
};
