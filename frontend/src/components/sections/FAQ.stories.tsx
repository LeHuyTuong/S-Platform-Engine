import type { Meta, StoryObj } from '@storybook/react-vite';
import { FAQ } from './FAQ';

const meta: Meta<typeof FAQ> = {
  title: 'Section/CâuHỏiThườngGặp',
  component: FAQ,
  parameters: {
    layout: 'fullscreen',
  },
};

export default meta;
type Story = StoryObj<typeof FAQ>;

export const MặcĐịnh: Story = {};
