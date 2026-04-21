import React, { useEffect, useRef } from 'react';
import { Terminal, Copy, ShieldAlert, CircleCheck, Play, Loader2, BarChart3 } from 'lucide-react';
import type { Job, JobState } from './types';
import { SpeedChart } from './SpeedChart';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface Props {
  job: Job | null;
  onClose?: () => void;
}

const StateBadge = ({ state }: { state: JobState }) => {
  const configs: Record<JobState, { label: string; color: string; icon: any; pulse?: boolean }> = {
    COMPLETED: { label: 'Hoàn thành', color: 'bg-success/10 text-success border-success/25', icon: CircleCheck },
    RUNNING: { label: 'Đang tải', color: 'bg-primary/10 text-primary border-primary/25', icon: Play, pulse: true },
    FAILED: { label: 'Lỗi', color: 'bg-danger/10 text-danger border-danger/25', icon: ShieldAlert },
    PENDING: { label: 'Đang chờ', color: 'bg-muted/10 text-muted border-muted/20', icon: Loader2 },
    ACCEPTED: { label: 'Đã nhận', color: 'bg-blue-500/10 text-blue-400 border-blue-500/25', icon: Loader2 },
    QUEUED: { label: 'Trong hàng đợi', color: 'bg-blue-500/10 text-blue-400 border-blue-500/25', icon: Loader2 },
    RETRY_WAIT: { label: 'Thử lại...', color: 'bg-warning/10 text-warning border-warning/25', icon: Loader2 },
    BLOCKED: { label: 'Bị chặn', color: 'bg-rose-500/10 text-rose-300 border-rose-500/25', icon: ShieldAlert },
  };

  const config = configs[state] || configs.PENDING;
  const Icon = config.icon;

  return (
    <span className={cn(
      "flex items-center gap-1.5 px-3 py-1 rounded-full text-[11px] font-bold uppercase tracking-wider border",
      config.color,
      config.pulse && "animate-pulse"
    )}>
      <Icon size={12} strokeWidth={3} />
      {config.label}
    </span>
  );
};

export const JobStatusDashboard: React.FC<Props> = ({ job }) => {
  const logEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (logEndRef.current) {
      logEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [job?.logs]);

  if (!job) {
    return (
      <div className="flex flex-col items-center justify-center p-12 bg-card-bg backdrop-blur-xl border border-border rounded-[20px] text-center">
        <div className="text-4xl mb-4 opacity-40">🚀</div>
        <h3 className="text-muted text-sm font-medium leading-relaxed">
          Chọn một Job trong danh sách<br />để xem trạng thái và log thời gian thực
        </h3>
      </div>
    );
  }

  const progress = job.progressPercent || 0;

  return (
    <div className="flex flex-col gap-6 animate-in fade-in slide-in-from-bottom-3 duration-500">
      {/* Header Card */}
      <div className="p-5 bg-card-bg backdrop-blur-xl border border-border rounded-[20px]">
        <div className="flex items-start justify-between gap-4 mb-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-bold text-muted uppercase tracking-widest">
                ID: {job.id.substring(0, 8)}
              </span>
              <StateBadge state={job.state} />
            </div>
            <h2 className="text-lg font-bold text-text truncate" title={job.videoTitle || job.url}>
              {job.videoTitle || job.playlistTitle || job.url}
            </h2>
          </div>
          <button className="p-2 hover:bg-white/5 rounded-lg text-muted transition-colors" title="Copy URL">
            <Copy size={18} />
          </button>
        </div>

        {/* Progress Bar */}
        <div className="space-y-2">
          <div className="flex justify-between items-end text-xs font-bold uppercase tracking-wide">
            <span className="text-primary">{job.downloadType}/{job.format.toUpperCase()}</span>
            <span className="text-primary/80">{Math.round(progress)}%</span>
          </div>
          <div className="h-2.5 bg-white/5 rounded-full overflow-hidden">
            <div 
              className="h-full bg-primary-gradient transition-all duration-700 ease-out rounded-full shadow-[0_0_12px_rgba(139,92,246,0.3)]"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      </div>

      {/* Speed Chart */}
      {job.state === 'RUNNING' && job.speedHistory && (
        <div className="space-y-2 animate-in fade-in zoom-in-95 duration-500">
          <div className="flex items-center gap-2 text-[10px] font-bold text-muted uppercase tracking-wider">
            <BarChart3 size={12} />
            Lịch sử tốc độ
          </div>
          <SpeedChart history={job.speedHistory} />
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-2 gap-3">
        <div className="p-4 bg-black/20 border border-border rounded-xl text-center">
          <div className="text-[10px] font-bold text-muted uppercase tracking-wider mb-1">Tốc độ</div>
          <div className="text-base font-black text-text">{job.downloadSpeed || '—'}</div>
        </div>
        <div className="p-4 bg-black/20 border border-border rounded-xl text-center">
          <div className="text-[10px] font-bold text-muted uppercase tracking-wider mb-1">Còn lại</div>
          <div className="text-base font-black text-text">{job.eta || '—'}</div>
        </div>
        {job.currentItem && (
          <div className="p-4 bg-black/20 border border-border rounded-xl text-center col-span-2">
            <div className="text-[10px] font-bold text-muted uppercase tracking-wider mb-1">Tiến độ Playlist</div>
            <div className="text-base font-black text-text">{job.currentItem} / {job.totalItems}</div>
          </div>
        )}
      </div>

      {/* Log Terminal */}
      <div className="flex flex-col flex-1 bg-black/30 border border-border rounded-[20px] overflow-hidden min-h-[300px]">
        <div className="flex items-center gap-2 px-4 py-3 border-bottom border-border bg-black/10">
          <Terminal size={14} className="text-muted" />
          <span className="text-[11px] font-bold text-muted uppercase tracking-wider">Log chi tiết</span>
        </div>
        <div className="flex-1 overflow-y-auto p-4 font-mono text-[11px] leading-relaxed text-slate-400 custom-scrollbar">
          {job.logs.length === 0 && <span className="opacity-30 italic">Đang chờ log...</span>}
          {job.logs.map((log, i) => {
             const isError = log.includes('ERROR') || log.includes('Lỗi');
             const isSuccess = log.includes('100%') || log.includes('Finished');
             return (
               <div key={i} className={cn(
                 "whitespace-pre-wrap break-all mb-0.5",
                 isError && "text-rose-400",
                 isSuccess && "text-emerald-400",
                 log.includes('[download]') && "text-violet-400"
               )}>
                 {log}
               </div>
             )
          })}
          <div ref={logEndRef} />
        </div>
      </div>
    </div>
  );
};
