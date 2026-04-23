import React from 'react';
import { Navigate, useNavigate, Outlet } from 'react-router-dom';
import { AdminLayout } from '../components/admin/layout/AdminLayout';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { useAuthSession } from '../features/downloader/hooks/useAuthSession';
import { logout } from '../api/auth';

interface AdminDashboardProps {
  disableAccessRedirects?: boolean;
}

const AdminDashboard: React.FC<AdminDashboardProps> = ({ disableAccessRedirects = false }) => {
  const navigate = useNavigate();
  const { session, loading: sessionLoading, error: sessionError } = useAuthSession();
  const [loggingOut, setLoggingOut] = React.useState(false);

  React.useEffect(() => {
    if (disableAccessRedirects || sessionLoading) {
      return;
    }

    if (sessionError && sessionError.status !== 401) {
      return;
    }

    if (sessionError?.status === 401 || !session?.authenticated) {
      window.location.assign('/');
      return;
    }

    if (session.role && session.role !== 'ADMIN') {
      navigate('/app/downloader', { replace: true });
    }
  }, [disableAccessRedirects, navigate, session, sessionError, sessionLoading]);

  async function handleLogout() {
    setLoggingOut(true);

    try {
      await logout();
    } catch {
      // Redirect anyway because the session may already be invalidated server-side.
    } finally {
      window.location.assign('/');
    }
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
        window.location.reload();
      }}
      refreshing={false}
    >
      <div className="min-h-full pb-12">
        <Outlet context={{ session }} />
      </div>
    </AdminLayout>
  );
};

export default AdminDashboard;
