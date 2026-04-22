import type { DownloadType, PlatformOption, SourceTypeOption } from '../../api/downloaderTypes';

export const PLATFORM_OPTIONS: Array<{ value: PlatformOption; label: string }> = [
  { value: 'AUTO', label: 'Tự nhận diện' },
  { value: 'YOUTUBE', label: 'YouTube' },
  { value: 'TIKTOK', label: 'TikTok' },
  { value: 'INSTAGRAM', label: 'Instagram' },
  { value: 'FACEBOOK', label: 'Facebook' },
];

export const SOURCE_TYPE_OPTIONS: Array<{ value: SourceTypeOption; label: string }> = [
  { value: 'AUTO', label: 'Tự suy luận' },
  { value: 'DIRECT_URL', label: 'URL trực tiếp' },
  { value: 'PLAYLIST', label: 'Playlist' },
  { value: 'PROFILE', label: 'Hồ sơ / Kênh' },
];

export const DOWNLOAD_TYPE_OPTIONS: Array<{ value: DownloadType; label: string }> = [
  { value: 'VIDEO', label: 'Video' },
  { value: 'AUDIO', label: 'Âm thanh' },
];

export const VIDEO_FORMAT_OPTIONS = [
  { value: 'mp4', label: 'MP4' },
  { value: 'mkv', label: 'MKV' },
];

export const AUDIO_FORMAT_OPTIONS = [
  { value: 'mp3', label: 'MP3' },
  { value: 'm4a', label: 'M4A' },
];

export const VIDEO_QUALITY_OPTIONS = [
  { value: 'best', label: 'Tốt nhất' },
  { value: '1080', label: '1080p' },
  { value: '720', label: '720p' },
  { value: '480', label: '480p' },
];
