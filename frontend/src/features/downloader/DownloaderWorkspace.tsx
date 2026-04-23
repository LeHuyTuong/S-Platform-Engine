import { useEffect, useState } from 'react';
import { RefreshCcw, ShieldCheck } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { submitSourceRequest } from '../../api/sourceRequests';
import type { AuthSession, SubmitSourceRequestPayload } from '../../api/downloaderTypes';
import type { ApiClientError } from '../../api/types';
import { toApiClientError } from '../../api/types';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { Button } from '../../components/common/Button';
import { Container } from '../../components/common/Container';
import { JobDetailPanel } from './components/JobDetailPanel';
import { JobsList } from './components/JobsList';
import { SourceRequestForm } from './components/SourceRequestForm';
import { SourceRequestList } from './components/SourceRequestList';
import { TelegramConfigCard } from './components/TelegramConfigCard';
import { GoogleDriveConfigCard } from './components/GoogleDriveConfigCard';
import { useAuthSession } from './hooks/useAuthSession';
import { useJobDetail } from './hooks/useJobDetail';
import { useJobs } from './hooks/useJobs';
import { useSourceRequests } from './hooks/useSourceRequests';
import { logout } from '../../api/auth';

interface AuthenticatedWorkspaceProps {
  session: AuthSession;
}

const AuthenticatedWorkspace = ({ session }: AuthenticatedWorkspaceProps) => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [submitError, setSubmitError] = useState<ApiClientError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const selectedJobId = searchParams.get('job');
  const {
    items: sourceRequests,
    loading: sourceRequestsLoading,
    refreshing: sourceRequestsRefreshing,
    error: sourceRequestsError,
    hasPending,
    refetch: refetchSourceRequests,
    prependSourceRequest,
  } = useSourceRequests({
    page: 0,
    size: 10,
  });
  const {
    items: jobs,
    meta: jobsMeta,
    loading: jobsLoading,
    refreshing: jobsRefreshing,
    error: jobsError,
    refetch: refetchJobs,
    upsertJob,
    prependJob,
  } = useJobs({
    page: 0,
    size: 20,
  });
  const {
    job,
    files,
    loading: jobLoading,
    refreshing: jobRefreshing,
    error: jobError,
    filesLoading,
    filesError,
    isPolling,
    refetch: refetchJob,
    refetchFiles,
  } = useJobDetail({
    jobId: selectedJobId,
  });

  useEffect(() => {
    if (!job) {
      return;
    }

    upsertJob(job);
  }, [job, upsertJob]);

  useEffect(() => {
    if (!hasPending && !isPolling) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void refetchJobs(true);
    }, 5_000);

    return () => window.clearInterval(intervalId);
  }, [hasPending, isPolling, refetchJobs]);

  function updateSelectedJob(jobId: string) {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set('job', jobId);
    setSearchParams(nextSearchParams, { replace: true });
  }

  async function handleSubmit(payload: SubmitSourceRequestPayload) {
    setSubmitting(true);
    setSubmitError(null);

    try {
      const response = await submitSourceRequest(payload);
      prependSourceRequest(response.data);

      const primaryJob = response.data.jobs[0];
      if (primaryJob) {
        prependJob(primaryJob);
        updateSelectedJob(primaryJob.id);
      } else {
        void refetchJobs(true);
      }
    } catch (error) {
      const apiError = toApiClientError(error);
      setSubmitError(apiError);
      throw apiError;
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <section className="grid gap-4 lg:grid-cols-3">
        <div className="rounded-[24px] border border-white/8 bg-card-bg/70 p-5 backdrop-blur-xl">
          <p className="text-[11px] font-bold tracking-[0.18em] text-muted uppercase">Tài khoản</p>
          <div className="mt-3 flex items-center gap-3">
            <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 p-3 text-emerald-300">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-semibold text-text">{session.email}</p>
              <p className="text-xs text-muted">Quyền hạn: {session.role}</p>
            </div>
          </div>
        </div>

        <div className="rounded-[24px] border border-white/8 bg-card-bg/70 p-5 backdrop-blur-xl">
          <p className="text-[11px] font-bold tracking-[0.18em] text-muted uppercase">Hạn mức hôm nay</p>
          <div className="mt-3 flex items-baseline gap-2">
            <p className="text-2xl font-black text-text">{session.jobsToday ?? 0}</p>
            <p className="text-sm text-muted">/ {session.dailyQuota ?? 0} yêu cầu</p>
          </div>
          <p className="mt-1 text-xs text-muted">Số lượt tải bạn đã sử dụng trong ngày</p>
        </div>

        <div className="rounded-[24px] border border-white/8 bg-card-bg/70 p-5 backdrop-blur-xl">
          <p className="text-[11px] font-bold tracking-[0.18em] text-muted uppercase">Trạng thái xử lý</p>
          <p className="mt-3 text-2xl font-black text-text">
            {hasPending || isPolling ? (
              <span className="flex items-center gap-2">
                <span className="relative flex h-3 w-3">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75"></span>
                  <span className="relative inline-flex h-3 w-3 rounded-full bg-primary"></span>
                </span>
                Đang theo dõi
              </span>
            ) : (
              'Hệ thống sẵn sàng'
            )}
          </p>
          <div className="mt-3">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                void refetchSourceRequests();
                void refetchJobs();
              }}
              className="h-8 px-3 text-xs"
            >
              <RefreshCcw className="mr-2 h-3.5 w-3.5" />
              Làm mới dữ liệu
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <div className="space-y-6">
          <SourceRequestForm isSubmitting={submitting} submitError={submitError} onSubmit={handleSubmit} />
          <SourceRequestList
            requests={sourceRequests}
            loading={sourceRequestsLoading}
            refreshing={sourceRequestsRefreshing}
            error={sourceRequestsError}
            selectedJobId={selectedJobId}
            onRetry={() => void refetchSourceRequests()}
            onSelectJob={updateSelectedJob}
          />
        </div>

        <div className="space-y-6">
          <JobsList
            jobs={jobs}
            meta={jobsMeta}
            loading={jobsLoading}
            refreshing={jobsRefreshing}
            error={jobsError}
            selectedJobId={selectedJobId}
            onRetry={() => void refetchJobs()}
            onSelectJob={updateSelectedJob}
          />
          <JobDetailPanel
            job={job}
            files={files}
            loading={jobLoading}
            refreshing={jobRefreshing}
            error={jobError}
            filesLoading={filesLoading}
            filesError={filesError}
            onRetry={() => void refetchJob()}
            onRetryFiles={() => void refetchFiles()}
          />
        </div>
      </section>

      {session.canManageRuntimeSettings && (
        <section className="space-y-6 pt-6">
          <div className="flex items-center gap-3 px-2">
            <h2 className="text-xl font-black tracking-tight text-white uppercase">Cấu hình & Tích hợp</h2>
            <div className="h-px flex-1 bg-gradient-to-r from-white/10 to-transparent" />
          </div>
          
          <div className="grid gap-6 lg:grid-cols-2">
            <TelegramConfigCard />
            <GoogleDriveConfigCard />
          </div>
        </section>
      )}
    </>
  );
};

export const DownloaderWorkspace = () => {
  const { session, loading: sessionLoading, error: sessionError, refetch: refetchSession } = useAuthSession();
  const isAuthenticated = Boolean(session?.authenticated);
  const [loggingOut, setLoggingOut] = useState(false);

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      // ignore
    } finally {
      window.location.assign('/');
    }
  }


  return (
    <div className="min-h-screen bg-bg text-text">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_left,rgba(139,92,246,0.14),transparent_30%),radial-gradient(circle_at_bottom_right,rgba(16,185,129,0.10),transparent_28%)]" />

      <header className="sticky top-0 z-40 border-b border-white/5 bg-bg/80 backdrop-blur-xl">
        <Container className="flex flex-col gap-4 py-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <nav className="flex items-center gap-2 text-xs font-medium text-muted">
              <a href="/" className="transition-colors hover:text-primary">
                Trang chủ
              </a>
              <span className="text-white/20">/</span>
              <span className="text-text">Tải video</span>
            </nav>
            <h1 className="mt-3 text-3xl font-black tracking-tight text-text sm:text-4xl">Bàn làm việc Tải Video</h1>

          </div>

          <div className="flex items-center gap-3">
            {!isAuthenticated || !session ? (
              <Button variant="primary" size="sm" href="/login">
                Đăng nhập ngay
              </Button>
            ) : (
              <div className="flex items-center gap-3">
                <div className="hidden text-right md:block">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-bold text-text">{session.email}</p>
                    <button
                      onClick={() => void handleLogout()}
                      disabled={loggingOut}
                      className="text-[10px] font-bold text-primary hover:underline uppercase tracking-tight"
                    >
                      {loggingOut ? 'Đang thoát...' : 'Đăng xuất'}
                    </button>
                  </div>
                  <p className="text-[10px] font-medium text-muted uppercase tracking-wider text-left">
                    {session.role}
                  </p>
                </div>
                <div className="h-10 w-10 rounded-full border border-white/10 bg-white/5 p-0.5">
                  <div className="flex h-full w-full items-center justify-center rounded-full bg-primary/20 text-primary font-bold">
                    {session.email?.[0].toUpperCase()}
                  </div>
                </div>
              </div>
            )}
          </div>
        </Container>
      </header>

      <main className="relative z-10 py-8 sm:py-10">
        <Container className="space-y-8">
          {sessionLoading ? (
            <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
              <EmptyState
                type="loading"
                title="Đang kiểm tra phiên làm việc"
                description="Hệ thống đang xác thực danh tính của bạn. Vui lòng chờ trong giây lát."
                className="min-h-[240px]"
              />
            </section>
          ) : sessionError ? (
            <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
              <EmptyState
                type="error"
                title="Không thể kết nối"
                description="Đã có lỗi xảy ra khi kiểm tra quyền truy cập của bạn."
                action={{ label: 'Thử lại ngay', onClick: () => void refetchSession() }}
                className="min-h-[240px]"
              />
            </section>
          ) : !isAuthenticated || !session ? (
            <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 backdrop-blur-xl">
              <EmptyState
                title="Bạn chưa đăng nhập"
                description="Vui lòng đăng nhập để sử dụng công cụ tải video và quản lý các yêu cầu của bạn."
                action={{
                  label: 'Đăng nhập để bắt đầu',
                  onClick: () => window.location.assign('/login'),
                }}
                className="min-h-[260px]"
              />
            </section>
          ) : (
            <AuthenticatedWorkspace session={session} />
          )}
        </Container>
      </main>
    </div>
  );
};
