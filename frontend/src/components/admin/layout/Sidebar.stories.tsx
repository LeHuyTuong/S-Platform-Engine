import type { Meta, StoryObj } from '@storybook/react';
import { Sidebar } from './Sidebar';
import { useState } from 'react';

const meta: Meta<typeof Sidebar> = {
  title: 'Admin/Layout/Sidebar',
  component: Sidebar,
  parameters: {
    layout: 'fullscreen',
  },
};

export default meta;
type Story = StoryObj<typeof Sidebar>;

const SidebarTemplate = (args: any) => {
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

export const Expanded: Story = {
  render: (args) => <SidebarTemplate {...args} isCollapsed={false} />,
};

export const Collapsed: Story = {
  render: (args) => <SidebarTemplate {...args} isCollapsed={true} />,
};
