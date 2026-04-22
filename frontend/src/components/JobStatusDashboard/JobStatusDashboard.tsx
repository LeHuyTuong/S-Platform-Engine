import { useEffect, useRef } from 'react';
import {
  BarChart3,
  CircleCheck,
  Copy,
  Loader2,
  Play,
  ShieldAlert,
  Terminal,
  type LucideIcon,
} from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { SpeedChart } from './SpeedChart';
import type { Job, JobState } from './types';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface Props {
  job: Job | null;
  onClose?: () => void;
}

const STATE_CONFIGS: Record<
  JobState,
  { label: string; color: string; icon: LucideIcon; pulse?: boolean }
> = {
  ACCEPTED: {
    label: 'Đã nhận yêu cầu',
    color: 'border-blue-500/25 bg-blue-500/10 text-blue-300',
    icon: Loader2,
    pulse: true,
  },
  RESOLVING: {
    label: 'Đang phân giải nguồn',
    color: 'border-sky-500/25 bg-sky-500/10 text-sky-300',
    icon: Loader2,
    pulse: true,
  },
  PENDING: {
    label: 'Đang chờ',
    color: 'border-muted/20 bg-muted/10 text-muted',
    icon: Loader2,
    pulse: true,
  },
  QUEUED: {
    label: 'Trong hàng đợi',
    color: 'border-blue-500/25 bg-blue-500/10 text-blue-300',
    icon: Loader2,
    pulse: true,
  },
  RUNNING: {
    label: 'Đang tải',
    color: 'border-primary/25 bg-primary/10 text-primary',
    icon: Play,
    pulse: true,
  },
  POST_PROCESSING: {
    label: 'Đang hậu xử lý',
    color: 'border-warning/25 bg-warning/10 text-warning',
    icon: Loader2,
    pulse: true,
  },
  COMPLETED: {
    label: 'Hoàn thành',
    color: 'border-success/25 bg-success/10 text-success',
    icon: CircleCheck,
  },
  FAILED: {
    label: 'Thất bại',
    color: 'border-danger/25 bg-danger/10 text-danger',
    icon: ShieldAlert,
  },
  BLOCKED: {
    label: 'Bị chặn',
    color: 'border-rose-500/25 bg-rose-500/10 text-rose-300',
    icon: ShieldAlert,
  },
  RETRY_WAIT: {
    label: 'Chờ thử lại',
    color: 'border-warning/25 bg-warning/10 text-warning',
    icon: Loader2,
    pulse: true,
  },
};

const StateBadge = ({ state }: { state: JobState }) => {
  const config = STATE_CONFIGS[state];
  const Icon = config.icon;

  return (
    <span
      className={cn(
        'flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-bold tracking-wider uppercase',
        config.color,
        config.pulse && 'animate-pulse',
      )}
    >
      <Icon size={12} strokeWidth={3} />
      {config.label}
    </span>
  );
};

export const JobStatusDashboard = ({ job }: Props) => {
  const logEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [job?.logs]);

  if (!job) {
    return (
      <div className="flex flex-col items-center justify-center rounded-[20px] border border-border bg-card-bg p-12 text-center backdrop-blur-xl">
        <div className="mb-4 text-4xl opacity-40">🚀</div>
        <h3 className="text-sm leading-relaxed font-medium text-muted">
          Chọn một job trong danh sách
          <br />
          để xem trạng thái và log thời gian thực
        </h3>
      </div>
    );
  }

  const progress = job.progressPercent || 0;

  return (
    <div className="animate-in fade-in slide-in-from-bottom-3 flex flex-col gap-6 duration-500">
      <div className="rounded-[20px] border border-border bg-card-bg p-5 backdrop-blur-xl">
        <div className="mb-4 flex items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            <div className="mb-1 flex items-center gap-2">
              <span className="text-xs font-bold tracking-widest text-muted uppercase">ID: {job.id.slice(0, 8)}</span>
              <StateBadge state={job.state} />
            </div>
            <h2 className="truncate text-lg font-bold text-text" title={job.videoTitle || job.url}>
              {job.videoTitle || job.playlistTitle || job.url}
            </h2>
          </div>
          <button className="rounded-lg p-2 text-muted transition-colors hover:bg-white/5" title="Sao chép URL" type="button">
            <Copy size={18} />
          </button>
        </div>

        <div className="space-y-2">
          <div className="flex items-end justify-between text-xs font-bold tracking-wide uppercase">
            <span className="text-primary">
              {job.downloadType}/{job.format.toUpperCase()}
            </span>
            <span className="text-primary/80">{Math.round(progress)}%</span>
          </div>
          <div className="h-2.5 overflow-hidden rounded-full bg-white/5">
            <div
              className="h-full rounded-full bg-primary-gradient shadow-[0_0_12px_rgba(139,92,246,0.3)] transition-all duration-700 ease-out"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      </div>

      {job.state === 'RUNNING' && job.speedHistory && job.speedHistory.length > 0 ? (
        <div className="animate-in fade-in zoom-in-95 space-y-2 duration-500">
          <div className="flex items-center gap-2 text-[10px] font-bold tracking-wider text-muted uppercase">
            <BarChart3 size={12} />
            Lịch sử tốc độ
          </div>
          <SpeedChart history={job.speedHistory} />
        </div>
      ) : null}

      <div className="grid grid-cols-2 gap-3">
        <div className="rounded-xl border border-border bg-black/20 p-4 text-center">
          <div className="mb-1 text-[10px] font-bold tracking-wider text-muted uppercase">Tốc độ</div>
          <div className="text-base font-black text-text">{job.downloadSpeed || '-'}</div>
        </div>
        <div className="rounded-xl border border-border bg-black/20 p-4 text-center">
          <div className="mb-1 text-[10px] font-bold tracking-wider text-muted uppercase">ETA</div>
          <div className="text-base font-black text-text">{job.eta || '-'}</div>
        </div>
        {job.currentItem && job.totalItems ? (
          <div className="col-span-2 rounded-xl border border-border bg-black/20 p-4 text-center">
            <div className="mb-1 text-[10px] font-bold tracking-wider text-muted uppercase">Tiến độ playlist</div>
            <div className="text-base font-black text-text">
              {job.currentItem} / {job.totalItems}
            </div>
          </div>
        ) : null}
      </div>

      <div className="flex min-h-[300px] max-h-[400px] flex-1 flex-col overflow-hidden rounded-[20px] border border-border bg-black/30">
        <div className="flex items-center gap-2 border-b border-border bg-black/10 px-4 py-3">
          <Terminal size={14} className="text-muted" />
          <span className="text-[11px] font-bold tracking-wider text-muted uppercase">Log chi tiết</span>
        </div>
        <div className="custom-scrollbar flex-1 overflow-y-auto p-4 font-mono text-[11px] leading-relaxed text-slate-400">
          {job.logs.length === 0 ? <span className="opacity-30 italic">Đang chờ log...</span> : null}
          {job.logs.map((logLine, index) => {
            const isError = logLine.includes('ERROR') || logLine.includes('Lỗi');
            const isSuccess = logLine.includes('100%') || logLine.includes('Finished');

            return (
              <div
                key={`${job.id}-${index}`}
                className={cn(
                  'mb-0.5 break-all whitespace-pre-wrap',
                  isError && 'text-rose-400',
                  isSuccess && 'text-emerald-400',
                  logLine.includes('[download]') && 'text-violet-400',
                )}
              >
                {logLine}
              </div>
            );
          })}
          <div ref={logEndRef} />
        </div>
      </div>
    </div>
  );
};
