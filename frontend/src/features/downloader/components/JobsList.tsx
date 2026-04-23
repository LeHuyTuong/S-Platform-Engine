import { RefreshCcw } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { EmptyState } from '../../../components/EmptyState/EmptyState';
import { Button } from '../../../components/common/Button';
import type { Job } from '../../../api/downloaderTypes';
import type { ApiMeta, ApiClientError } from '../../../api/types';
import {
  formatDateTime,
  getJobStateLabel,
  getPlatformLabel,
  getStateTone,
  truncateMiddle,
  cleanVideoTitle,
} from '../utils';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface Props {
  jobs: Job[];
  meta: ApiMeta | null;
  loading: boolean;
  refreshing: boolean;
  error: ApiClientError | null;
  selectedJobId: string | null;
  onRetry: () => void;
  onSelectJob: (jobId: string) => void;
}

export const JobsList = ({
  jobs,
  meta,
  loading,
  refreshing,
  error,
  selectedJobId,
  onRetry,
  onSelectJob,
}: Props) => {
  if (loading && jobs.length === 0) {
    return (
      <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, index) => (
            <div
              key={`job-skeleton-${index}`}
              className="h-24 animate-pulse rounded-[24px] border border-white/5 bg-white/[0.03]"
            />
          ))}
        </div>
      </section>
    );
  }

  if (error && jobs.length === 0) {
    return (
      <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
        <EmptyState
          type="error"
          title="Không tải được danh sách job"
          description={error.message}
          action={{ label: 'Thử lại', onClick: onRetry }}
          className="min-h-[220px]"
        />
      </section>
    );
  }

  return (
    <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
      <div className="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-[11px] font-bold tracking-[0.18em] text-primary uppercase">Job của tôi</p>
          <h2 className="mt-2 text-2xl font-black tracking-tight text-text">Danh sách job của tôi</h2>
          <p className="mt-2 text-sm leading-6 text-muted">
            {meta
              ? `Trang ${meta.page + 1}/${Math.max(meta.totalPages, 1)} • ${meta.totalItems} job`
              : 'Hiển thị 20 job mới nhất.'}
          </p>
        </div>

        <div className="flex items-center gap-3">
          {refreshing ? <span className="text-xs font-semibold text-muted">Đang đồng bộ...</span> : null}
          <Button variant="ghost" size="sm" onClick={onRetry}>
            <RefreshCcw className="mr-2 h-4 w-4" />
            Làm mới
          </Button>
        </div>
      </div>

      {error ? (
        <div className="mb-4 rounded-2xl border border-amber-400/20 bg-amber-400/10 px-4 py-3 text-sm text-amber-100">
          Lần đồng bộ gần nhất thất bại: {error.message}
        </div>
      ) : null}

      {jobs.length === 0 ? (
        <EmptyState
          title="Chưa có job nào"
          description="Sau khi gửi yêu cầu nguồn thành công, job của bạn sẽ xuất hiện ở đây."
          className="min-h-[220px]"
        />
      ) : (
        <div className="space-y-3">
          {jobs.map((job) => {
            const rawTitle = job.videoTitle || job.playlistTitle || job.url;
            const title = cleanVideoTitle(rawTitle);

            return (
              <button
                key={job.id}
                className={cn(
                  'w-full rounded-[24px] border p-4 text-left transition-colors',
                  selectedJobId === job.id
                    ? 'border-primary/40 bg-primary/10'
                    : 'border-white/8 bg-black/15 hover:border-primary/20 hover:bg-white/[0.03]',
                )}
                type="button"
                onClick={() => onSelectJob(job.id)}
              >
                <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span
                        className={cn(
                          'rounded-full border px-3 py-1 text-[11px] font-bold tracking-[0.18em] uppercase',
                          getStateTone(job.state),
                        )}
                      >
                        {getJobStateLabel(job.state)}
                      </span>
                      <span className="rounded-full border border-white/10 bg-white/[0.03] px-3 py-1 text-[11px] font-semibold text-muted">
                        {getPlatformLabel(job.platform)}
                      </span>
                    </div>

                    <div className="mt-3 text-sm font-semibold text-text" title={title}>
                      {truncateMiddle(title, 50, 16)}
                    </div>

                    <div className="mt-2 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted">
                      <span>Job: {job.id.slice(0, 10)}</span>
                      <span>{job.downloadType}/{job.format.toUpperCase()}</span>
                      <span>Tạo lúc: {formatDateTime(job.createdAt)}</span>
                      {job.totalItems ? <span>{job.currentItem ?? 0}/{job.totalItems} item</span> : null}
                    </div>
                  </div>

                  <div className="w-full max-w-sm rounded-2xl border border-white/8 bg-white/[0.03] p-4">
                    <div className="mb-2 flex items-center justify-between text-xs font-semibold text-muted">
                      <span>{job.downloadSpeed || 'Chưa có tốc độ'}</span>
                      <span>{Math.round(job.progressPercent)}%</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-white/8">
                      <div
                        className={cn(
                          'h-full rounded-full transition-[width] duration-500',
                          job.state === 'FAILED' || job.state === 'BLOCKED'
                            ? 'bg-rose-400'
                            : job.state === 'COMPLETED'
                              ? 'bg-emerald-400'
                              : 'bg-primary',
                        )}
                        style={{ width: `${Math.max(job.progressPercent, 4)}%` }}
                      />
                    </div>
                    <div className="mt-3 flex items-center justify-between text-xs text-muted">
                      <span>{job.eta || 'ETA chưa có'}</span>
                      <span>{job.errorMessage ? 'Có lỗi cần xem chi tiết' : 'Xem log và file tải'}</span>
                    </div>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </section>
  );
};
