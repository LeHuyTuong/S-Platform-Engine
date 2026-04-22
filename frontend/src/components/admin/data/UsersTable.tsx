import React from 'react';
import type { AdminUser } from '../../../api/adminTypes';
import { Badge } from '../atoms/Badge';
import { formatAdminShortDate, getRoleLabel, getRoleVariant } from '../../../features/admin/utils';

interface UsersTableProps {
  users: AdminUser[];
  loading?: boolean;
  error?: string | null;
}

export const UsersTable: React.FC<UsersTableProps> = ({
  users,
  loading = false,
  error = null,
}) => {
  if (loading) {
    return <div className="text-sm text-slate-400">Đang tải danh sách người dùng...</div>;
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-rose-400/20 bg-rose-500/10 p-4 text-sm text-rose-200">
        {error}
      </div>
    );
  }

  if (users.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-white/10 bg-white/[0.02] p-4 text-sm text-slate-400">
        Chưa có tài khoản nào để hiển thị.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[520px] border-collapse text-left">
        <thead>
          <tr className="border-b border-white/5">
            <th className="px-3 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Email</th>
            <th className="px-3 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Vai trò</th>
            <th className="px-3 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Trạng thái</th>
            <th className="px-3 pb-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-500">Tạo lúc</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-white/5">
          {users.map((user) => (
            <tr key={user.id}>
              <td className="px-3 py-4 text-sm font-medium text-white">{user.email}</td>
              <td className="px-3 py-4">
                <Badge variant={getRoleVariant(user.role)}>{getRoleLabel(user.role)}</Badge>
              </td>
              <td className="px-3 py-4">
                <Badge variant={user.enabled ? 'success' : 'danger'}>{user.enabled ? 'Đang hoạt động' : 'Bị khóa'}</Badge>
              </td>
              <td className="px-3 py-4 text-sm text-slate-400">{formatAdminShortDate(user.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
