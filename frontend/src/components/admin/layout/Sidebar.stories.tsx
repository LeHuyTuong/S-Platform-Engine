import { useState, type ComponentProps } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Sidebar } from './Sidebar';

const meta: Meta<typeof Sidebar> = {
  title: 'Admin/Layout/ThanhBên',
  component: Sidebar,
  parameters: {
    layout: 'fullscreen',
  },
  args: {
    currentUserEmail: 'admin@test.com',
  },
};

export default meta;
type Story = StoryObj<typeof Sidebar>;

type SidebarStoryProps = ComponentProps<typeof Sidebar>;

const SidebarTemplate = (args: SidebarStoryProps) => {
  const [isCollapsed, setIsCollapsed] = useState(args.isCollapsed);

  return (
    <div className="h-screen bg-bg">
      <Sidebar
        {...args}
        isCollapsed={isCollapsed}
        onToggle={() => setIsCollapsed(!isCollapsed)}
      />
    </div>
  );
};

export const MởRộng: Story = {
  render: (args) => <SidebarTemplate {...args} isCollapsed={false} />,
};

export const ThuGọn: Story = {
  render: (args) => <SidebarTemplate {...args} isCollapsed={true} />,
};
