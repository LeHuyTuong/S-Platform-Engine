import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter } from 'react-router-dom';
import AdminDashboard from './AdminDashboard';

const meta: Meta<typeof AdminDashboard> = {
  title: 'Trang/BảngĐiềuKhiểnAdmin',
  component: AdminDashboard,
  parameters: {
    layout: 'fullscreen',
  },
  decorators: [
    (Story) => (
      <MemoryRouter initialEntries={['/admin']}>
        <Story />
      </MemoryRouter>
    ),
  ],
  args: {
    disableAccessRedirects: true,
  },
};

export default meta;
type Story = StoryObj<typeof AdminDashboard>;

export const MặcĐịnh: Story = {};
