import type { Meta, StoryObj } from '@storybook/react-vite';
import { EmptyState } from './EmptyState';
import { FileSearch } from 'lucide-react';

const meta: Meta<typeof EmptyState> = {
  title: 'Common/TrạngTháiRỗng',
  component: EmptyState,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof EmptyState>;

export const MặcĐịnh: Story = {
  args: {
    title: 'Chưa có bộ sưu tập nào',
    description: 'Bắt đầu bằng cách tạo bộ sưu tập đầu tiên của bạn để quản lý các video đã tải xuống.',
  },
};

export const CóHànhĐộng: Story = {
  args: {
    title: 'Không tìm thấy kết quả',
    description: 'Thử điều chỉnh bộ lọc của bạn hoặc tìm kiếm với từ khóa khác.',
    action: {
      label: 'Xóa tất cả bộ lọc',
      onClick: () => alert('Đã bấm hành động'),
    },
  },
};

export const ĐangTải: Story = {
  args: {
    type: 'loading',
    title: 'Đang tải dữ liệu...',
    description: 'Hệ thống đang đồng bộ hóa thông tin từ máy chủ.',
  },
};

export const BáoLỗi: Story = {
  args: {
    type: 'error',
    title: 'Mất kết nối máy chủ',
    description: 'Không thể tải được danh sách job. Vui lòng kiểm tra lại đường truyền internet của bạn.',
    action: {
      label: 'Thử lại ngay',
      onClick: () => alert('Đã bấm thử lại'),
    },
  },
};

export const IconTùyChỉnh: Story = {
  args: {
    icon: FileSearch,
    title: 'Thiếu file cấu hình',
    description: 'Vui lòng tải lên file cookie.txt để tiếp tục sử dụng tính năng này.',
  },
};
