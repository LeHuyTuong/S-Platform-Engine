import React from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AdminLayout } from '../components/admin/layout/AdminLayout';
import { StatsGrid } from '../components/admin/data/StatsGrid';
import { FilterBar } from '../components/admin/data/FilterBar';
import { DataTable } from '../components/admin/data/DataTable';
import { UsersTable } from '../components/admin/data/UsersTable';
import { SettingsPanel } from '../components/admin/data/SettingsPanel';
import { Card } from '../components/admin/atoms/Card';
import { Pagination } from '../components/admin/atoms/Pagination';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { useAuthSession } from '../features/downloader/hooks/useAuthSession';
import { useAdminDashboard } from '../features/admin/hooks/useAdminDashboard';
import { useAdminJobs } from '../features/admin/hooks/useAdminJobs';
import { useAdminUsers } from '../features/admin/hooks/useAdminUsers';
import { useAdminSettings } from '../features/admin/hooks/useAdminSettings';
import { backfillAdminJobTitles, resubmitAdminJob, updateAdminSettings } from '../api/admin';
import type { AdminSettings } from '../api/adminTypes';
import type { JobState, JobStatus, Platform } from '../api/downloaderTypes';
import { logout } from '../api/auth';
import { toApiClientError } from '../api/types';
import { formatMegabytes } from '../features/admin/utils';

interface AdminDashboardProps {
  disableAccessRedirects?: boolean;
}

const pageSize = 20;

const AdminDashboard: React.FC<AdminDashboardProps> = ({ disableAccessRedirects = false }) => {
  const navigate = useNavigate();
  const { session, loading: sessionLoading, error: sessionError } = useAuthSession();
  const [page, setPage] = React.useState(0);
  const [stateFilter, setStateFilter] = React.useState<JobState | ''>('');
  const [statusFilter, setStatusFilter] = React.useState<JobStatus | ''>('');
  const [platformFilter, setPlatformFilter] = React.useState<Platform | ''>('');
  const [feedback, setFeedback] = React.useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [resubmittingJobId, setResubmittingJobId] = React.useState<string | null>(null);
  const [savingSettings, setSavingSettings] = React.useState(false);
  const [backfillingTitles, setBackfillingTitles] = React.useState(false);
  const [loggingOut, setLoggingOut] = React.useState(false);

  const isAdmin = session?.authenticated && session.role === 'ADMIN';

  const dashboard = useAdminDashboard({ enabled: Boolean(isAdmin) });
  const jobs = useAdminJobs({
    enabled: Boolean(isAdmin),
    page,
    size: pageSize,
    state: stateFilter,
    status: statusFilter,
    platform: platformFilter,
  });
  const users = useAdminUsers({ enabled: Boolean(isAdmin) });
  const settings = useAdminSettings({ enabled: Boolean(isAdmin) });

  React.useEffect(() => {
    if (disableAccessRedirects || sessionLoading) {
      return;
    }

    if (sessionError && sessionError.status !== 401) {
      return;
    }

    if (sessionError?.status === 401 || !session?.authenticated) {
      window.location.assign('/login');
      return;
    }

    if (session.role && session.role !== 'ADMIN') {
      navigate('/app/downloader', { replace: true });
    }
  }, [disableAccessRedirects, navigate, session, sessionError, sessionLoading]);

  const isRefreshing = dashboard.refreshing || jobs.refreshing || users.refreshing || settings.refreshing;

  async function handleRefreshAll() {
    await Promise.allSettled([
      dashboard.refetch(),
      jobs.refetch(),
      users.refetch(),
      settings.refetch(),
    ]);
  }

  async function handleLogout() {
    setLoggingOut(true);

    try {
      await logout();
    } catch {
      // Redirect anyway because the session may already be invalidated server-side.
    } finally {
      window.location.assign('/login');
    }
  }

  async function handleResubmit(jobId: string) {
    setResubmittingJobId(jobId);

    try {
      const response = await resubmitAdminJob(jobId);
      setFeedback({
        type: 'success',
        message: `Đã resubmit job. Job mới: ${response.data.jobId}.`,
      });
      await Promise.allSettled([dashboard.refetch(), jobs.refetch()]);
    } catch (error) {
      setFeedback({
        type: 'error',
        message: toApiClientError(error).message,
      });
    } finally {
      setResubmittingJobId(null);
    }
  }

  async function handleSaveSettings(nextSettings: Omit<AdminSettings, 'diskUsageMb'>) {
    setSavingSettings(true);

    try {
      await updateAdminSettings(nextSettings);
      setFeedback({
        type: 'success',
        message: 'Đã lưu cấu hình quản trị.',
      });
      await Promise.allSettled([settings.refetch(), dashboard.refetch()]);
    } catch (error) {
      setFeedback({
        type: 'error',
        message: toApiClientError(error).message,
      });
    } finally {
      setSavingSettings(false);
    }
  }

  async function handleBackfillTitles() {
    setBackfillingTitles(true);

    try {
      const response = await backfillAdminJobTitles();
      setFeedback({
        type: 'success',
        message: `Đã backfill ${response.data.updated} job.`,
      });
      await Promise.allSettled([jobs.refetch(), dashboard.refetch()]);
    } catch (error) {
      setFeedback({
        type: 'error',
        message: toApiClientError(error).message,
      });
    } finally {
      setBackfillingTitles(false);
    }
  }

  function handleResetFilters() {
    setStateFilter('');
    setStatusFilter('');
    setPlatformFilter('');
    setPage(0);
  }

  if (disableAccessRedirects && sessionError && !session) {
    return (
      <div className="min-h-screen bg-[#0b1020] p-8">
        <EmptyState
          type="error"
          title="Không thể tải phiên đăng nhập"
          description={sessionError.message}
        />
      </div>
    );
  }

  if (sessionLoading || loggingOut) {
    return (
      <div className="min-h-screen bg-[#0b1020] p-8">
        <EmptyState
          type="loading"
          title={loggingOut ? 'Đang đăng xuất' : 'Đang kiểm tra quyền truy cập'}
          description="Hệ thống đang đồng bộ phiên đăng nhập của bạn."
        />
      </div>
    );
  }

  if (sessionError && sessionError.status !== 401 && !session) {
    return (
      <div className="min-h-screen bg-[#0b1020] p-8">
        <EmptyState
          type="error"
          title="Không thể tải quyền truy cập admin"
          description={sessionError.message}
          action={{
            label: 'Thử lại',
            onClick: () => window.location.reload(),
          }}
        />
      </div>
    );
  }

  if (!disableAccessRedirects && (sessionError?.status === 401 || !session?.authenticated)) {
    return (
      <div className="min-h-screen bg-[#0b1020] p-8">
        <EmptyState
          type="loading"
          title="Đang chuyển tới trang đăng nhập"
          description="Phiên hiện tại chưa hợp lệ cho trang quản trị."
        />
      </div>
    );
  }

  if (!disableAccessRedirects && session?.authenticated && session.role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-[#0b1020] p-8">
        <EmptyState
          type="loading"
          title="Đang chuyển về workspace người dùng"
          description="Tài khoản hiện tại không có quyền vào trang quản trị."
        />
      </div>
    );
  }

  if (disableAccessRedirects && session?.authenticated && session.role !== 'ADMIN') {
    return (
      <div className="min-h-screen bg-[#0b1020] p-8">
        <EmptyState
          type="error"
          title="Không có quyền truy cập"
          description="Story hoặc môi trường hiện tại đang dùng tài khoản không phải ADMIN."
        />
      </div>
    );
  }

  if (!session?.authenticated) {
    return <Navigate replace to="/" />;
  }

  return (
    <AdminLayout
      currentUserEmail={session.email}
      currentUserRole={session.role}
      onLogout={() => {
        void handleLogout();
      }}
      onRefresh={() => {
        void handleRefreshAll();
      }}
      refreshing={isRefreshing}
    >
      <div className="space-y-8">
        <section id="tong-quan" className="space-y-6">
          <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
            <div>
              <h1 className="text-3xl font-black tracking-tight text-white">Trung tâm quản trị hệ thống</h1>
              <p className="mt-2 max-w-3xl text-sm text-slate-400">
                Giao diện này dành riêng cho ADMIN để theo dõi thống kê thật, quản lý toàn bộ job,
                xem danh sách người dùng, chỉnh runtime settings và chạy các thao tác vận hành v1.
              </p>
            </div>

            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <div className="rounded-2xl border border-white/5 bg-white/[0.03] px-4 py-3">
                <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Phiên hiện tại</p>
                <p className="mt-2 text-sm font-semibold text-white">{session.email}</p>
                <p className="mt-1 text-xs text-slate-400">Vai trò: {session.role}</p>
              </div>
              <div className="rounded-2xl border border-white/5 bg-white/[0.03] px-4 py-3">
                <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Dung lượng lưu trữ</p>
                <p className="mt-2 text-sm font-semibold text-white">
                  {dashboard.stats ? formatMegabytes(dashboard.stats.diskUsageMb) : 'Đang tải'}
                </p>
                <p className="mt-1 text-xs text-slate-400">Số liệu đọc trực tiếp từ backend admin API</p>
              </div>
            </div>
          </div>

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

          {dashboard.stats ? (
            <StatsGrid stats={dashboard.stats} />
          ) : dashboard.loading ? (
            <EmptyState
              type="loading"
              title="Đang tải thống kê quản trị"
              description="Backend đang trả về các chỉ số toàn hệ thống."
            />
          ) : (
            <EmptyState
              type="error"
              title="Không tải được thống kê"
              description={dashboard.error?.message ?? 'Không thể đọc dữ liệu dashboard.'}
              action={{
                label: 'Thử lại',
                onClick: () => {
                  void dashboard.refetch();
                },
              }}
            />
          )}
        </section>

        <section id="jobs">
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
        </section>

        <section className="grid grid-cols-1 gap-8 xl:grid-cols-[1.1fr_0.9fr]">
          <Card
            title="Người dùng"
            subtitle="Danh sách tài khoản thật từ `/api/v1/admin/users`."
            className="h-full"
          >
            <div id="users">
              <UsersTable
                users={users.users}
                loading={users.loading}
                error={users.error?.message ?? null}
              />
            </div>
          </Card>

          <Card
            title="Cài đặt runtime"
            subtitle="Đọc và lưu qua `/api/v1/admin/settings`, kèm tác vụ backfill tiêu đề."
            className="h-full"
          >
            <div id="settings">
              <SettingsPanel
                settings={settings.settings}
                loading={settings.loading}
                saving={savingSettings}
                backfilling={backfillingTitles}
                onSave={(nextSettings) => {
                  void handleSaveSettings(nextSettings);
                }}
                onBackfillTitles={() => {
                  void handleBackfillTitles();
                }}
              />
            </div>
          </Card>
        </section>
      </div>
    </AdminLayout>
  );
};

export default AdminDashboard;
