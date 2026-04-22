import React from 'react';
import { Filter, RotateCcw } from 'lucide-react';
import type { JobState, JobStatus, Platform } from '../../../api/downloaderTypes';
import { Button } from '../../common/Button';

interface FilterBarProps {
  state: JobState | '';
  status: JobStatus | '';
  platform: Platform | '';
  refreshing?: boolean;
  onStateChange: (value: JobState | '') => void;
  onStatusChange: (value: JobStatus | '') => void;
  onPlatformChange: (value: Platform | '') => void;
  onReset: () => void;
  onRefresh: () => void;
}

const stateOptions: Array<{ label: string; value: JobState | '' }> = [
  { label: 'Tất cả state', value: '' },
  { label: 'Đã nhận', value: 'ACCEPTED' },
  { label: 'Đang phân giải', value: 'RESOLVING' },
  { label: 'Trong hàng đợi', value: 'QUEUED' },
  { label: 'Đang chạy', value: 'RUNNING' },
  { label: 'Hậu xử lý', value: 'POST_PROCESSING' },
  { label: 'Hoàn thành', value: 'COMPLETED' },
  { label: 'Thất bại', value: 'FAILED' },
  { label: 'Chờ thử lại', value: 'RETRY_WAIT' },
  { label: 'Bị chặn', value: 'BLOCKED' },
];

const statusOptions: Array<{ label: string; value: JobStatus | '' }> = [
  { label: 'Tất cả status', value: '' },
  { label: 'Chờ xử lý', value: 'PENDING' },
  { label: 'Đang chạy', value: 'RUNNING' },
  { label: 'Hoàn thành', value: 'COMPLETED' },
  { label: 'Thất bại', value: 'FAILED' },
];

const platformOptions: Array<{ label: string; value: Platform | '' }> = [
  { label: 'Tất cả nền tảng', value: '' },
  { label: 'YouTube', value: 'YOUTUBE' },
  { label: 'TikTok', value: 'TIKTOK' },
  { label: 'Instagram', value: 'INSTAGRAM' },
  { label: 'Facebook', value: 'FACEBOOK' },
];

export const FilterBar: React.FC<FilterBarProps> = ({
  state,
  status,
  platform,
  refreshing = false,
  onStateChange,
  onStatusChange,
  onPlatformChange,
  onReset,
  onRefresh,
}) => {
  return (
    <div className="mb-6 flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
      <div className="grid w-full grid-cols-1 gap-3 md:grid-cols-3">
        <label className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">State xử lý</span>
          <select
            value={state}
            onChange={(event) => onStateChange(event.target.value as JobState | '')}
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          >
            {stateOptions.map((option) => (
              <option key={option.label} value={option.value} className="bg-slate-950 text-white">
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Status tổng quát</span>
          <select
            value={status}
            onChange={(event) => onStatusChange(event.target.value as JobStatus | '')}
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          >
            {statusOptions.map((option) => (
              <option key={option.label} value={option.value} className="bg-slate-950 text-white">
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Nền tảng</span>
          <select
            value={platform}
            onChange={(event) => onPlatformChange(event.target.value as Platform | '')}
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          >
            {platformOptions.map((option) => (
              <option key={option.label} value={option.value} className="bg-slate-950 text-white">
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="flex items-center gap-2">
        <Button variant="ghost" size="sm" onClick={onReset}>
          <Filter size={14} className="mr-2" />
          Xóa bộ lọc
        </Button>
        <Button variant="secondary" size="sm" onClick={onRefresh} disabled={refreshing}>
          <RotateCcw size={14} className={refreshing ? 'mr-2 animate-spin' : 'mr-2'} />
          {refreshing ? 'Đang tải lại' : 'Làm mới danh sách'}
        </Button>
      </div>
    </div>
  );
};
