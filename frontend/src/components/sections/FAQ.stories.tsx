import type { Meta, StoryObj } from '@storybook/react';
import { FAQ } from './FAQ';

const meta: Meta<typeof FAQ> = {
  title: 'Sections/FAQ',
  component: FAQ,
  parameters: {
    layout: 'fullscreen',
  },
};

export default meta;
type Story = StoryObj<typeof FAQ>;

export const Default: Story = {};
