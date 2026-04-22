import type { Meta, StoryObj } from '@storybook/react-vite';
import { Features } from './Features';

const meta: Meta<typeof Features> = {
  title: 'Section/TínhNăng',
  component: Features,
  parameters: {
    layout: 'fullscreen',
  },
};

export default meta;
type Story = StoryObj<typeof Features>;

export const MặcĐịnh: Story = {};
