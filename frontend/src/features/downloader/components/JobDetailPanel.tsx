import { CheckCircle2, Download, RefreshCcw } from 'lucide-react';
import { EmptyState } from '../../../components/EmptyState/EmptyState';
import { JobStatusDashboard } from '../../../components/JobStatusDashboard/JobStatusDashboard';
import type { Job as DashboardJob } from '../../../components/JobStatusDashboard/types';
import { Button } from '../../../components/common/Button';
import type { Job, JobFile } from '../../../api/downloaderTypes';
import type { ApiClientError } from '../../../api/types';
import { formatFileSize, cleanVideoTitle } from '../utils';

interface Props {
  job: Job | null;
  files: JobFile[];
  loading: boolean;
  refreshing: boolean;
  error: ApiClientError | null;
  filesLoading: boolean;
  filesError: ApiClientError | null;
  onRetry: () => void;
  onRetryFiles: () => void;
}

function toDashboardJob(job: Job | null): DashboardJob | null {
  if (!job) {
    return null;
  }

  return {
    id: job.id,
    url: job.url,
    videoTitle: cleanVideoTitle(job.videoTitle),
    playlistTitle: cleanVideoTitle(job.playlistTitle),
    downloadType: job.downloadType,
    format: job.format,
    state: job.state,
    status: job.status,
    progressPercent: job.progressPercent,
    downloadSpeed: job.downloadSpeed,
    eta: job.eta,
    currentItem: job.currentItem,
    totalItems: job.totalItems,
    errorMessage: job.errorMessage,
    logs: job.logs,
  };
}

export const JobDetailPanel = ({
  job,
  files,
  loading,
  refreshing,
  error,
  filesLoading,
  filesError,
  onRetry,
  onRetryFiles,
}: Props) => {
  const primaryDownloadableFile = files.find((file) => Boolean(file.downloadUrl)) ?? null;
  const downloadableCount = files.filter((file) => Boolean(file.downloadUrl)).length;

  if (loading && !job) {
    return (
      <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
        <EmptyState
          type="loading"
          title="Đang tải chi tiết job"
          description="Backend đang trả log và progress mới nhất."
        />
      </section>
    );
  }

  if (error && !job) {
    return (
      <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
        <EmptyState
          type="error"
          title="Không tải được chi tiết job"
          description={error.message}
          action={{ label: 'Thử lại', onClick: onRetry }}
        />
      </section>
    );
  }

  return (
    <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
      <div className="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-[11px] font-bold tracking-[0.18em] text-primary uppercase">Chi tiết job</p>
          <h2 className="mt-2 text-2xl font-black tracking-tight text-text">Trạng thái, log và file đầu ra</h2>
          <p className="mt-2 text-sm leading-6 text-muted">
            {job
              ? `Chi tiết cho job ${job.id.slice(0, 10)}${refreshing ? ' • đang polling' : ''}`
              : 'Chọn một job ở bên trái để xem tiến trình.'}
          </p>
        </div>

        {job ? (
          <Button variant="ghost" size="sm" onClick={onRetry}>
            <RefreshCcw className="mr-2 h-4 w-4" />
            Đồng bộ ngay
          </Button>
        ) : null}
      </div>

      {error && job ? (
        <div className="mb-4 rounded-2xl border border-amber-400/20 bg-amber-400/10 px-4 py-3 text-sm text-amber-100">
          Chi tiết job chưa đồng bộ được ở lần gần nhất: {error.message}
        </div>
      ) : null}

      <JobStatusDashboard job={toDashboardJob(job)} />

      {job?.state === 'COMPLETED' ? (
        <div className="mt-6 rounded-[24px] border border-white/8 bg-black/15 p-5">
          <div className="mb-4 rounded-3xl border border-emerald-400/20 bg-emerald-400/10 p-4">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div className="min-w-0">
                <div className="flex items-center gap-2 text-emerald-200">
                  <CheckCircle2 className="h-5 w-5" />
                  <p className="text-sm font-bold uppercase tracking-[0.18em]">Sẵn sàng tải file</p>
                </div>
                <h3 className="mt-2 text-xl font-black text-text">File đầu ra đã sẵn sàng để tải về máy</h3>
                <p className="mt-2 max-w-2xl text-sm leading-6 text-emerald-50/85">
                  Job đã hoàn tất. Bấm nút tải file để browser lưu về máy của bạn. Nếu browser không hỏi vị trí lưu,
                  file sẽ vào thư mục Downloads mặc định.
                </p>
                <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold text-emerald-100/90">
                  <span className="rounded-full border border-emerald-300/20 bg-emerald-300/10 px-3 py-1">
                    Trạng thái: HOÀN TẤT
                  </span>
                  <span className="rounded-full border border-emerald-300/20 bg-emerald-300/10 px-3 py-1">
                    {downloadableCount} file có thể tải
                  </span>
                </div>
              </div>

              {primaryDownloadableFile?.downloadUrl ? (
                <div className="flex shrink-0 flex-col gap-2">
                  <Button
                    href={primaryDownloadableFile.downloadUrl}
                    download={primaryDownloadableFile.name}
                    className="justify-center"
                    data-testid="job-primary-download"
                  >
                    <Download className="mr-2 h-4 w-4" />
                    Tải file ngay
                  </Button>
                  <p className="text-center text-xs text-emerald-100/80">Ưu tiên file đầu tiên backend trả về</p>
                </div>
              ) : null}
            </div>
          </div>

          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="text-lg font-bold text-text">File tải xuống</h3>
              <p className="mt-1 text-sm text-muted">Link dùng trực tiếp `downloadUrl` do backend trả về.</p>
            </div>

            {filesError ? (
              <Button variant="ghost" size="sm" onClick={onRetryFiles}>
                <RefreshCcw className="mr-2 h-4 w-4" />
                Thử lại file list
              </Button>
            ) : null}
          </div>

          {filesLoading ? (
            <EmptyState
              type="loading"
              title="Đang tải danh sách file"
              description="Backend đã hoàn tất job, frontend đang gọi `/api/v1/jobs/{id}/files`."
              className="min-h-[200px]"
            />
          ) : filesError ? (
            <EmptyState
              type="error"
              title="Không tải được danh sách file"
              description={filesError.message}
              action={{ label: 'Thử lại', onClick: onRetryFiles }}
              className="min-h-[200px]"
            />
          ) : files.length === 0 ? (
            <EmptyState
              title="Chưa có file đầu ra"
              description="Job đã completed nhưng backend chưa trả file nào cho endpoint file listing."
              className="min-h-[200px]"
            />
          ) : (
            <div className="space-y-3" data-testid="job-output-files">
              {files.map((file) => (
                <div
                  key={`${file.name}-${file.downloadUrl}`}
                  className="flex flex-col gap-3 rounded-2xl border border-white/8 bg-white/[0.03] p-4 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="min-w-0">
                    <div className="break-all text-sm font-semibold text-text">{cleanVideoTitle(file.name)}</div>
                    <div className="mt-1 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted">
                      <span>{file.type || 'Không rõ'}</span>
                      <span>{file.contentType || 'content-type chưa có'}</span>
                      <span>{formatFileSize(file.size)}</span>
                    </div>
                  </div>

                  {file.downloadUrl ? (
                    <Button href={file.downloadUrl} download={file.name} data-testid="job-file-download">
                      <Download className="mr-2 h-4 w-4" />
                      Tải file
                    </Button>
                  ) : (
                    <span className="text-xs font-semibold text-muted">Backend chưa trả downloadUrl</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}
    </section>
  );
};
