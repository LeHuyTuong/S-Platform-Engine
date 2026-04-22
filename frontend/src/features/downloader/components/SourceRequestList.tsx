import { Clock3, RefreshCcw } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { EmptyState } from '../../../components/EmptyState/EmptyState';
import { Button } from '../../../components/common/Button';
import type { SourceRequest } from '../../../api/downloaderTypes';
import type { ApiClientError } from '../../../api/types';
import {
  formatDateTime,
  getPlatformLabel,
  getSourceRequestStateLabel,
  getSourceTypeLabel,
  getStateTone,
  truncateMiddle,
} from '../utils';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface Props {
  requests: SourceRequest[];
  loading: boolean;
  refreshing: boolean;
  error: ApiClientError | null;
  selectedJobId: string | null;
  onRetry: () => void;
  onSelectJob: (jobId: string) => void;
}

export const SourceRequestList = ({
  requests,
  loading,
  refreshing,
  error,
  selectedJobId,
  onRetry,
  onSelectJob,
}: Props) => {
  if (loading && requests.length === 0) {
    return (
      <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <p className="text-[11px] font-bold tracking-[0.18em] text-primary uppercase">Yêu cầu nguồn</p>
            <h2 className="mt-2 text-xl font-black tracking-tight text-text">Lịch sử submit gần nhất</h2>
          </div>
        </div>

        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <div
              key={`source-request-skeleton-${index}`}
              className="h-28 animate-pulse rounded-3xl border border-white/5 bg-white/[0.03]"
            />
          ))}
        </div>
      </section>
    );
  }

  if (error && requests.length === 0) {
    return (
      <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <p className="text-[11px] font-bold tracking-[0.18em] text-primary uppercase">Yêu cầu nguồn</p>
            <h2 className="mt-2 text-xl font-black tracking-tight text-text">Lịch sử submit gần nhất</h2>
          </div>
        </div>

        <EmptyState
          type="error"
          title="Không tải được yêu cầu nguồn"
          description={error.message}
          action={{ label: 'Thử lại', onClick: onRetry }}
          className="min-h-[220px]"
        />
      </section>
    );
  }

  return (
    <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-[11px] font-bold tracking-[0.18em] text-primary uppercase">Yêu cầu nguồn</p>
          <h2 className="mt-2 text-xl font-black tracking-tight text-text">Lịch sử submit gần nhất</h2>
          <p className="mt-2 text-sm leading-6 text-muted">
            Hiển thị 10 yêu cầu mới nhất để bạn biết URL nào đã tạo ra job nào.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {refreshing ? (
            <div className="flex items-center gap-2 text-xs font-semibold text-muted">
              <Clock3 className="h-4 w-4" />
              Đang làm mới
            </div>
          ) : null}

          <Button variant="ghost" size="sm" onClick={onRetry}>
            <RefreshCcw className="mr-2 h-4 w-4" />
            Làm mới
          </Button>
        </div>
      </div>

      {error ? (
        <div className="mb-4 rounded-2xl border border-amber-400/20 bg-amber-400/10 px-4 py-3 text-sm text-amber-100">
          Không thể đồng bộ lần gần nhất: {error.message}
        </div>
      ) : null}

      {requests.length === 0 ? (
        <EmptyState
          title="Chưa có yêu cầu nguồn nào"
          description="Gửi một URL ở phía trên để bắt đầu theo dõi quá trình tạo job."
          className="min-h-[220px]"
        />
      ) : (
        <div className="space-y-3">
          {requests.map((request) => (
            <article
              key={request.id}
              className="rounded-[24px] border border-white/8 bg-black/15 p-4 transition-colors hover:border-primary/20"
            >
              <div className="flex flex-col gap-4">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <span
                        className={cn(
                          'rounded-full border px-3 py-1 text-[11px] font-bold tracking-[0.18em] uppercase',
                          getStateTone(request.state),
                        )}
                      >
                        {getSourceRequestStateLabel(request.state)}
                      </span>
                      <span className="rounded-full border border-white/10 bg-white/[0.03] px-3 py-1 text-[11px] font-semibold text-muted">
                        {getPlatformLabel(request.platform)}
                      </span>
                      <span className="rounded-full border border-white/10 bg-white/[0.03] px-3 py-1 text-[11px] font-semibold text-muted">
                        {getSourceTypeLabel(request.sourceType)}
                      </span>
                    </div>

                    <div className="mt-3 break-all text-sm font-semibold text-text" title={request.sourceUrl}>
                      {truncateMiddle(request.sourceUrl, 40, 18)}
                    </div>

                    <div className="mt-2 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted">
                      <span>Yêu cầu: {request.id.slice(0, 10)}</span>
                      <span>Tạo lúc: {formatDateTime(request.createdAt)}</span>
                      <span>Số lượng đã phân giải: {request.resolvedCount ?? 0}</span>
                    </div>
                  </div>

                  <div className="rounded-2xl border border-white/8 bg-white/[0.03] px-4 py-3 text-xs leading-5 text-muted lg:max-w-[220px]">
                    {request.jobs.length > 0
                      ? `${request.jobs.length} job đã được tạo từ yêu cầu nguồn này.`
                      : request.state === 'ACCEPTED' || request.state === 'RESOLVING'
                        ? 'Backend vẫn đang phân giải hoặc chờ worker tạo job.'
                        : 'Chưa có job gắn với yêu cầu nguồn này.'}
                  </div>
                </div>

                {request.errorMessage ? (
                  <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
                    {request.errorMessage}
                  </div>
                ) : null}

                {request.blockedReason ? (
                  <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
                    Lý do bị chặn: {request.blockedReason}
                  </div>
                ) : null}

                {request.jobs.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {request.jobs.map((job) => (
                      <button
                        key={job.id}
                        className={cn(
                          'rounded-2xl border px-3 py-2 text-left text-xs transition-colors',
                          selectedJobId === job.id
                            ? 'border-primary/40 bg-primary/15 text-primary'
                            : 'border-white/8 bg-white/[0.03] text-text hover:border-primary/20 hover:bg-primary/10',
                        )}
                        type="button"
                        onClick={() => onSelectJob(job.id)}
                      >
                        <div className="font-bold">{job.videoTitle || job.url}</div>
                        <div className="mt-1 text-muted">
                          {job.downloadType}/{job.format.toUpperCase()} • {Math.round(job.progressPercent)}%
                        </div>
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};
