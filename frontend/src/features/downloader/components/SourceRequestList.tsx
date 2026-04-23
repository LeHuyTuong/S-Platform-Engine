import { useState, useMemo } from 'react';
import { Clock3, RefreshCcw, Search, X } from 'lucide-react';
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
  cleanVideoTitle,
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
  const [searchQuery, setSearchQuery] = useState('');

  const filteredRequests = useMemo(() => {
    if (!searchQuery.trim()) return requests;
    const query = searchQuery.toLowerCase().trim();
    return requests.filter(
      (req) =>
        req.id.toLowerCase().includes(query) ||
        req.sourceUrl.toLowerCase().includes(query) ||
        getSourceRequestStateLabel(req.state).toLowerCase().includes(query)
    );
  }, [requests, searchQuery]);
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
    <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl transition-all">
      <div className="mb-6 flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
        <div className="max-w-md">
          <p className="text-[11px] font-bold tracking-[0.18em] text-primary uppercase">Yêu cầu nguồn</p>
          <h2 className="mt-2 text-xl font-black tracking-tight text-text">Lịch sử submit gần nhất</h2>

        </div>

        <div className="flex flex-col gap-4 sm:items-end">
          <div className="flex items-center gap-3">
            {refreshing ? (
              <div className="flex items-center gap-2 text-xs font-semibold text-muted">
                <Clock3 className="h-4 w-4 animate-spin-slow" />
                Đang làm mới
              </div>
            ) : null}

            <Button variant="ghost" size="sm" onClick={onRetry} className="rounded-full">
              <RefreshCcw className="mr-2 h-4 w-4" />
              Làm mới
            </Button>
          </div>

          <div className="relative group w-full sm:w-64">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted transition-colors group-focus-within:text-primary" />
            <input
              type="text"
              placeholder="Tìm kiếm URL hoặc ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-2xl border border-white/8 bg-white/[0.03] py-2.5 pl-11 pr-10 text-sm text-text placeholder:text-muted/50 focus:border-primary/50 focus:bg-white/[0.05] focus:outline-none transition-all"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-1 text-muted hover:bg-white/10 hover:text-text"
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </div>
        </div>
      </div>

      {error ? (
        <div className="mb-4 rounded-2xl border border-amber-400/20 bg-amber-400/10 px-4 py-3 text-sm text-amber-100">
          Không thể đồng bộ lần gần nhất: {error.message}
        </div>
      ) : null}

      <div className="relative">
        <div 
          className={cn(
            "space-y-3 pr-2 transition-all",
            requests.length > 3 ? "max-h-[600px] overflow-y-auto custom-scrollbar" : ""
          )}
        >
          {filteredRequests.length === 0 ? (
            <EmptyState
              title={searchQuery ? "Không tìm thấy yêu cầu nào" : "Chưa có yêu cầu nguồn nào"}
              description={searchQuery ? `Không có kết quả nào khớp với "${searchQuery}"` : "Gửi một URL ở phía trên để bắt đầu theo dõi quá trình tạo job."}
              className="min-h-[220px]"
            />
          ) : (
            filteredRequests.map((request) => (
              <article
                key={request.id}
                className={cn(
                  "rounded-[24px] border border-white/8 bg-black/15 p-4 transition-all hover:border-primary/30 hover:bg-white/[0.02]",
                  selectedJobId && request.jobs.some(j => j.id === selectedJobId) ? "border-primary/20 bg-primary/[0.03]" : ""
                )}
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



                      <div className="mt-2 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted">
                        <span>ID: <code className="text-primary/70">{request.id.slice(0, 10)}</code></span>
                        <span>Tạo lúc: {formatDateTime(request.createdAt)}</span>
                        <span>Đã phân giải: {request.resolvedCount ?? 0}</span>
                      </div>
                    </div>

                    <div className="rounded-2xl border border-white/8 bg-white/[0.03] px-4 py-3 text-xs leading-5 text-muted lg:max-w-[220px] shrink-0">
                      {request.jobs.length > 0
                        ? `${request.jobs.length} job đã được tạo.`
                        : request.state === 'ACCEPTED' || request.state === 'RESOLVING'
                          ? 'Backend đang phân giải yêu cầu...'
                          : 'Chưa có job nào được tạo.'}
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
                            'rounded-2xl border px-3 py-2 text-left text-xs transition-all',
                            selectedJobId === job.id
                              ? 'border-primary/40 bg-primary/15 text-primary ring-1 ring-primary/20'
                              : 'border-white/8 bg-white/[0.03] text-text hover:border-primary/20 hover:bg-primary/10',
                          )}
                          type="button"
                          onClick={() => onSelectJob(job.id)}
                        >
                          <div className="font-bold">{cleanVideoTitle(job.videoTitle) || truncateMiddle(job.url, 20, 10)}</div>
                          <div className="mt-1 text-muted">
                            {job.downloadType}/{job.format.toUpperCase()} • {Math.round(job.progressPercent)}%
                          </div>
                        </button>
                      ))}
                    </div>
                  ) : null}
                </div>
              </article>
            ))
          )}
        </div>
        {requests.length > 3 && (
            <div className="pointer-events-none absolute bottom-0 left-0 right-0 h-12 bg-gradient-to-t from-card-bg/95 to-transparent rounded-b-[28px]" />
        )}
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .custom-scrollbar::-webkit-scrollbar {
          width: 6px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: rgba(255, 255, 255, 0.02);
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.1);
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: rgba(124, 58, 237, 0.3);
        }
      `}} />
    </section>
  );
};
