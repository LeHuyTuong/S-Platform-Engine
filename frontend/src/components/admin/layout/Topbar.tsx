import React from 'react';
import { LogOut, RefreshCw, Shield } from 'lucide-react';
import { Button } from '../../common/Button';

interface TopbarProps {
  sidebarWidth: number;
  currentUserEmail?: string | null;
  currentUserRole?: string | null;
  onLogout?: () => void;
  onRefresh?: () => void;
  refreshing?: boolean;
}

export const Topbar: React.FC<TopbarProps> = ({
  sidebarWidth,
  currentUserEmail,
  currentUserRole,
  onLogout,
  onRefresh,
  refreshing = false,
}) => {
  return (
    <header
      className="fixed right-0 top-0 z-30 flex h-16 items-center justify-between border-b border-white/5 bg-[#0b1020]/80 px-6 backdrop-blur-xl transition-all duration-300"
      style={{ left: sidebarWidth }}
    >
      <div>
        <p className="text-[11px] font-bold uppercase tracking-[0.2em] text-slate-500">Bảng điều khiển admin</p>
        <p className="mt-1 text-sm font-semibold text-white">
          {currentUserEmail ?? 'Đang đồng bộ phiên đăng nhập'}
          {currentUserRole ? <span className="ml-2 text-slate-400">({currentUserRole})</span> : null}
        </p>
      </div>

      <div className="flex items-center gap-3">
        <div className="hidden items-center gap-2 rounded-2xl border border-emerald-400/15 bg-emerald-500/10 px-3 py-2 text-xs font-semibold text-emerald-200 md:flex">
          <Shield size={14} />
          Quyền quản trị hệ thống
        </div>

        <Button
          variant="secondary"
          size="sm"
          onClick={onRefresh}
          disabled={refreshing}
          className="min-w-[122px]"
        >
          <RefreshCw size={14} className={refreshing ? 'mr-2 animate-spin' : 'mr-2'} />
          {refreshing ? 'Đang làm mới' : 'Làm mới dữ liệu'}
        </Button>

        <Button variant="ghost" size="sm" onClick={onLogout}>
          <LogOut size={14} className="mr-2" />
          Đăng xuất
        </Button>
      </div>
    </header>
  );
};
