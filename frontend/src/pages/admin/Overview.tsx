import React from 'react';
import { useOutletContext } from 'react-router-dom';
import { StatsGrid } from '../../components/admin/data/StatsGrid';
import { useAdminDashboard } from '../../features/admin/hooks/useAdminDashboard';
import { formatMegabytes } from '../../features/admin/utils';
import { EmptyState } from '../../components/EmptyState/EmptyState';

const Overview: React.FC = () => {
  const { session } = useOutletContext<{ session: { email: string; role: string } }>();
  const dashboard = useAdminDashboard({ enabled: true });

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-white">Trung tâm quản trị hệ thống</h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-400">
            Giao diện này dành riêng cho ADMIN để theo dõi thống kê thật, quản lý toàn bộ job,
            xem danh sách người dùng, chỉnh runtime settings và chạy các thao tác vận hành.
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
  );
};

export default Overview;
