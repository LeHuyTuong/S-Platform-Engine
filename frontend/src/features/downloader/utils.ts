import type { JobState, Platform, SourceRequestState, SourceType } from '../../api/downloaderTypes';

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function formatFileSize(bytes: number) {
  if (bytes <= 0) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bytes;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  return `${value.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

export function truncateMiddle(value: string, start = 26, end = 12) {
  if (value.length <= start + end + 3) {
    return value;
  }

  return `${value.slice(0, start)}...${value.slice(-end)}`;
}

export function getJobStateLabel(state: JobState) {
  switch (state) {
    case 'ACCEPTED':
      return 'Đã nhận';
    case 'RESOLVING':
      return 'Đang phân giải nguồn';
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
      return state;
  }
}

export function getSourceRequestStateLabel(state: SourceRequestState) {
  switch (state) {
    case 'ACCEPTED':
      return 'Đã nhận yêu cầu';
    case 'RESOLVING':
      return 'Đang phân giải';
    case 'RESOLVED':
      return 'Đã tạo job';
    case 'BLOCKED':
      return 'Bị chặn';
    case 'FAILED':
      return 'Thất bại';
    default:
      return state;
  }
}

export function getPlatformLabel(platform: Platform | null | undefined) {
  if (!platform) {
    return 'Tự nhận diện';
  }

  return {
    YOUTUBE: 'YouTube',
    TIKTOK: 'TikTok',
    INSTAGRAM: 'Instagram',
    FACEBOOK: 'Facebook',
  }[platform];
}

export function getSourceTypeLabel(sourceType: SourceType | null | undefined) {
  if (!sourceType) {
    return 'Tự suy luận';
  }

  return {
    DIRECT_URL: 'URL trực tiếp',
    PLAYLIST: 'Danh sách phát',
    PROFILE: 'Hồ sơ / Kênh',
  }[sourceType];
}

export function getStateTone(state: JobState | SourceRequestState) {
  if (state === 'COMPLETED' || state === 'RESOLVED') {
    return 'border-emerald-400/20 bg-emerald-400/10 text-emerald-300';
  }

  if (state === 'FAILED' || state === 'BLOCKED') {
    return 'border-rose-400/20 bg-rose-400/10 text-rose-300';
  }

  if (state === 'POST_PROCESSING' || state === 'RETRY_WAIT') {
    return 'border-amber-400/20 bg-amber-400/10 text-amber-300';
  }

  return 'border-primary/20 bg-primary/10 text-primary';
}

export function cleanVideoTitle(title: string | null | undefined): string {
  if (!title) return '';

  return title
    .replace(/^(NA|Unknown|N\/A)\s*-\s*/i, '') // Remove NA - or Unknown - prefixes
    .replace(/\s*\([^)]*\)/g, '') // Remove (Official Video), (4K Remaster), etc.
    .replace(/\s*\[[^\]]*\]/g, '') // Remove [dQw4w9WgXcQ], etc.
    .replace(/\.[a-z]{2,3}(\.[a-z]{2,3})?$/i, '') // Remove .en or .en.vtt or .mp4
    .trim();
}
