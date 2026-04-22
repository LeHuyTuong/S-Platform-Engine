import type { JobState, JobStatus, Platform } from '../../api/downloaderTypes';
import type { BadgeVariant } from '../../components/admin/atoms/Badge';

export function formatAdminDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function formatAdminShortDate(value?: string | null) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(value));
}

export function formatMegabytes(value: number) {
  return `${value.toFixed(1)} MB`;
}

export function getAdminJobStatusLabel(status?: JobStatus | null) {
  switch (status) {
    case 'PENDING':
      return 'Chờ xử lý';
    case 'RUNNING':
      return 'Đang chạy';
    case 'COMPLETED':
      return 'Hoàn thành';
    case 'FAILED':
      return 'Thất bại';
    default:
      return 'Chưa rõ';
  }
}

export function getAdminJobStatusVariant(status?: JobStatus | null): BadgeVariant {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'RUNNING':
      return 'info';
    case 'PENDING':
    default:
      return 'muted';
  }
}

export function getAdminJobStateLabel(state?: JobState | null) {
  switch (state) {
    case 'ACCEPTED':
      return 'Đã nhận';
    case 'RESOLVING':
      return 'Đang phân giải';
    case 'QUEUED':
      return 'Trong hàng đợi';
    case 'RUNNING':
      return 'Đang chạy';
    case 'POST_PROCESSING':
      return 'Hậu xử lý';
    case 'COMPLETED':
      return 'Hoàn thành';
    case 'FAILED':
      return 'Thất bại';
    case 'RETRY_WAIT':
      return 'Chờ thử lại';
    case 'BLOCKED':
      return 'Bị chặn';
    default:
      return state ?? 'Chưa rõ';
  }
}

export function getRoleLabel(role?: string | null) {
  switch (role) {
    case 'ADMIN':
      return 'Quản trị';
    case 'PUBLISHER':
      return 'Publisher';
    case 'USER':
      return 'Người dùng';
    default:
      return role ?? 'Chưa rõ';
  }
}

export function getRoleVariant(role?: string | null): BadgeVariant {
  switch (role) {
    case 'ADMIN':
      return 'danger';
    case 'PUBLISHER':
      return 'warning';
    case 'USER':
    default:
      return 'muted';
  }
}

export function getPlatformLabel(platform?: Platform | '' | null) {
  switch (platform) {
    case 'YOUTUBE':
      return 'YouTube';
    case 'TIKTOK':
      return 'TikTok';
    case 'INSTAGRAM':
      return 'Instagram';
    case 'FACEBOOK':
      return 'Facebook';
    default:
      return 'Tất cả nền tảng';
  }
}
