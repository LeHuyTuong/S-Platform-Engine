import type { Meta, StoryObj } from '@storybook/react-vite';
import { ApiClientError } from '../../../api/types';
import { SourceRequestForm } from './SourceRequestForm';

const meta: Meta<typeof SourceRequestForm> = {
  title: 'Downloader/FormYêuCầuNguồn',
  component: SourceRequestForm,
  parameters: {
    layout: 'fullscreen',
  },
  render: (args) => (
    <div className="min-h-screen bg-bg p-6 text-text">
      <div className="mx-auto max-w-3xl">
        <SourceRequestForm {...args} />
      </div>
    </div>
  ),
};

export default meta;
type Story = StoryObj<typeof SourceRequestForm>;

export const MặcĐịnh: Story = {
  args: {
    isSubmitting: false,
    submitError: null,
    onSubmit: async () => undefined,
  },
};

export const ĐangGửi: Story = {
  args: {
    isSubmitting: true,
    submitError: null,
    onSubmit: async () => undefined,
  },
};

export const LỗiTừBackend: Story = {
  args: {
    isSubmitting: false,
    submitError: new ApiClientError({
      status: 400,
      code: 'BUSINESS_ERROR',
      message: 'Vui lòng nhập URL',
      fieldErrors: [{ field: 'sourceUrl', message: 'Không được để trống.' }],
    }),
    onSubmit: async () => undefined,
  },
};
