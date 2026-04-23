import React from 'react';
import { Card } from '../../components/admin/atoms/Card';
import { FilterBar } from '../../components/admin/data/FilterBar';
import { DataTable } from '../../components/admin/data/DataTable';
import { Pagination } from '../../components/admin/atoms/Pagination';
import { useAdminJobs } from '../../features/admin/hooks/useAdminJobs';
import { resubmitAdminJob } from '../../api/admin';
import { toApiClientError } from '../../api/types';
import type { JobState, JobStatus, Platform } from '../../api/downloaderTypes';

const pageSize = 20;

const Jobs: React.FC = () => {
  const [page, setPage] = React.useState(0);
  const [stateFilter, setStateFilter] = React.useState<JobState | ''>('');
  const [statusFilter, setStatusFilter] = React.useState<JobStatus | ''>('');
  const [platformFilter, setPlatformFilter] = React.useState<Platform | ''>('');
  const [feedback, setFeedback] = React.useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [resubmittingJobId, setResubmittingJobId] = React.useState<string | null>(null);

  const jobs = useAdminJobs({
    enabled: true,
    page,
    size: pageSize,
    state: stateFilter,
    status: statusFilter,
    platform: platformFilter,
  });

  async function handleResubmit(jobId: string) {
    setResubmittingJobId(jobId);
    setFeedback(null);

    try {
      const response = await resubmitAdminJob(jobId);
      setFeedback({
        type: 'success',
        message: `Đã resubmit job. Job mới: ${response.data.jobId}.`,
      });
      void jobs.refetch();
    } catch (error) {
      setFeedback({
        type: 'error',
        message: toApiClientError(error).message,
      });
    } finally {
      setResubmittingJobId(null);
    }
  }

  function handleResetFilters() {
    setStateFilter('');
    setStatusFilter('');
    setPlatformFilter('');
    setPage(0);
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-black tracking-tight text-white">Quản lý công việc</h1>
      <p className="text-sm text-slate-400">Danh sách toàn bộ các job tải video trong hệ thống.</p>

      {feedback ? (
        <div
          className={`rounded-2xl border px-4 py-3 text-sm ${
            feedback.type === 'success'
              ? 'border-emerald-400/20 bg-emerald-500/10 text-emerald-200'
              : 'border-rose-400/20 bg-rose-500/10 text-rose-200'
          }`}
        >
          {feedback.message}
        </div>
      ) : null}

      <Card
        title="Công việc toàn hệ thống"
        subtitle="Danh sách thật từ `/api/v1/admin/jobs` với filter, phân trang và resubmit."
      >
        <FilterBar
          state={stateFilter}
          status={statusFilter}
          platform={platformFilter}
          refreshing={jobs.refreshing}
          onStateChange={(value) => {
            setStateFilter(value);
            setPage(0);
          }}
          onStatusChange={(value) => {
            setStatusFilter(value);
            setPage(0);
          }}
          onPlatformChange={(value) => {
            setPlatformFilter(value);
            setPage(0);
          }}
          onReset={handleResetFilters}
          onRefresh={() => {
            void jobs.refetch();
          }}
        />

        <DataTable
          jobs={jobs.jobs}
          loading={jobs.loading}
          error={jobs.error?.message ?? null}
          resubmittingJobId={resubmittingJobId}
          onResubmit={(jobId) => {
            void handleResubmit(jobId);
          }}
        />

        <Pagination
          currentPage={(jobs.meta?.page ?? 0) + 1}
          totalPages={Math.max(jobs.meta?.totalPages ?? 1, 1)}
          onPageChange={(nextPage) => setPage(Math.max(nextPage - 1, 0))}
        />
      </Card>
    </div>
  );
};

export default Jobs;
