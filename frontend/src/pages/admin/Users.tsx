import React from 'react';
import { Card } from '../../components/admin/atoms/Card';
import { UsersTable } from '../../components/admin/data/UsersTable';
import { useAdminUsers } from '../../features/admin/hooks/useAdminUsers';

const Users: React.FC = () => {
  const users = useAdminUsers({ enabled: true });

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-black tracking-tight text-white">Quản lý người dùng</h1>
      <p className="text-sm text-slate-400">Danh sách các tài khoản đang hoạt động trong hệ thống.</p>

      <Card
        title="Người dùng hệ thống"
        subtitle="Danh sách tài khoản thật từ `/api/v1/admin/users`."
      >
        <UsersTable
          users={users.users}
          loading={users.loading}
          error={users.error?.message ?? null}
        />
      </Card>
    </div>
  );
};

export default Users;
