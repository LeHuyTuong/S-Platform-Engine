import React from 'react';
import { ExternalLink, RefreshCcw } from 'lucide-react';
import type { AdminJob } from '../../../api/adminTypes';
import { Badge } from '../atoms/Badge';
import {
  formatAdminDateTime,
  getAdminJobStateLabel,
  getAdminJobStatusLabel,
  getAdminJobStatusVariant,
  getPlatformLabel,
  getRoleLabel,
  getRoleVariant,
} from '../../../features/admin/utils';

interface DataTableProps {
  jobs: AdminJob[];
  loading?: boolean;
  error?: string | null;
  resubmittingJobId?: string | null;
  onResubmit: (jobId: string) => void;
}

export const DataTable: React.FC<DataTableProps> = ({
  jobs,
  loading = false,
  error = null,
  resubmittingJobId = null,
  onResubmit,
}) => {
  if (loading) {
    return (
      <div className="rounded-2xl border border-white/5 bg-white/[0.02] p-6 text-sm text-slate-400">
        Đang tải danh sách công việc quản trị...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-rose-400/20 bg-rose-500/10 p-6 text-sm text-rose-200">
        {error}
      </div>
    );
  }

  if (jobs.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-white/10 bg-white/[0.02] p-6 text-sm text-slate-400">
        Không có job nào khớp với bộ lọc hiện tại.
      </div>
    );
  }

  return (
    <div className="w-full overflow-x-auto">
      <table className="w-full min-w-[1080px] border-collapse text-left">
        <thead>
          <tr className="border-b border-white/5">
            <th className="px-4 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Job</th>
            <th className="px-4 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Chủ sở hữu</th>
            <th className="px-4 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Nền tảng</th>
            <th className="px-4 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Status</th>
            <th className="px-4 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">State</th>
            <th className="px-4 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Tiến độ</th>
            <th className="px-4 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Thời điểm</th>
            <th className="px-4 pb-4 text-right text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Tác vụ</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-white/5">
          {jobs.map((job) => {
            const title = job.videoTitle || job.playlistTitle || job.url;

            return (
              <tr key={job.id} className="transition-colors hover:bg-white/[0.02]">
                <td className="px-4 py-5 align-top">
                  <div className="flex flex-col gap-2">
                    <span className="max-w-[280px] truncate text-sm font-semibold text-white">{title}</span>
                    <span className="text-xs text-slate-400">{job.id}</span>
                    <a
                      href={job.url}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1 text-xs text-sky-300 hover:text-sky-200"
                    >
                      Mở URL nguồn
                      <ExternalLink size={12} />
                    </a>
                  </div>
                </td>
                <td className="px-4 py-5 align-top">
                  <div className="flex flex-col gap-2">
                    <span className="text-sm font-semibold text-white">{job.ownerEmail ?? 'Không rõ'}</span>
                    <Badge variant={getRoleVariant(job.ownerRole)}>{getRoleLabel(job.ownerRole)}</Badge>
                  </div>
                </td>
                <td className="px-4 py-5 align-top">
                  <div className="flex flex-col gap-2 text-sm text-white">
                    <span>{getPlatformLabel(job.platform)}</span>
                    <span className="text-xs text-slate-400">
                      {job.downloadType} / {job.format}
                    </span>
                  </div>
                </td>
                <td className="px-4 py-5 align-top">
                  <Badge variant={getAdminJobStatusVariant(job.status)}>{getAdminJobStatusLabel(job.status)}</Badge>
                </td>
                <td className="px-4 py-5 align-top">
                  <span className="text-sm text-slate-200">{getAdminJobStateLabel(job.state)}</span>
                </td>
                <td className="px-4 py-5 align-top">
                  <div className="w-40">
                    <div className="mb-2 flex items-center justify-between text-xs text-slate-400">
                      <span>{job.progressPercent}%</span>
                      <span>{job.downloadSpeed || job.eta || 'Chưa có dữ liệu'}</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-white/5">
                      <div
                        className={`h-full rounded-full ${
                          job.status === 'FAILED' ? 'bg-rose-400' : 'bg-sky-400'
                        }`}
                        style={{ width: `${Math.max(0, Math.min(job.progressPercent, 100))}%` }}
                      />
                    </div>
                    {job.errorMessage ? (
                      <p className="mt-2 max-w-[240px] text-xs text-rose-300">{job.errorMessage}</p>
                    ) : null}
                  </div>
                </td>
                <td className="px-4 py-5 align-top">
                  <div className="flex flex-col gap-1 text-xs text-slate-400">
                    <span>{formatAdminDateTime(job.createdAt)}</span>
                    <span>{job.sourceRequestId ? `Source request: ${job.sourceRequestId}` : 'Không có source request'}</span>
                  </div>
                </td>
                <td className="px-4 py-5 text-right align-top">
                  <button
                    onClick={() => onResubmit(job.id)}
                    disabled={resubmittingJobId === job.id}
                    className="inline-flex items-center rounded-xl border border-sky-400/20 bg-sky-500/10 px-3 py-2 text-xs font-semibold text-sky-200 transition hover:bg-sky-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <RefreshCcw size={13} className={resubmittingJobId === job.id ? 'mr-2 animate-spin' : 'mr-2'} />
                    {resubmittingJobId === job.id ? 'Đang resubmit' : 'Resubmit'}
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
