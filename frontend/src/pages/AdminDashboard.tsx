import React, { useState, useEffect } from 'react';
import { AdminLayout } from '../components/admin/layout/AdminLayout';
import { StatsGrid } from '../components/admin/data/StatsGrid';
import { FilterBar } from '../components/admin/data/FilterBar';
import { DataTable } from '../components/admin/data/DataTable';
import { Card } from '../components/admin/atoms/Card';
import { Pagination } from '../components/admin/atoms/Pagination';
import { EmptyState } from '../components/EmptyState/EmptyState';

const AdminDashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const error = null; // Simulated error state
  const [isEmpty, setIsEmpty] = useState(false);

  useEffect(() => {
    // Simulate loading
    const timer = setTimeout(() => {
      setLoading(false);
    }, 1500);
    return () => clearTimeout(timer);
  }, []);

  return (
    <AdminLayout>
      <div className="space-y-8 animate-in fade-in duration-500">
        {/* Page Header */}
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-black text-text uppercase tracking-tight">Bảng điều khiển</h1>
          <p className="text-xs font-bold text-muted uppercase tracking-[0.2em]">Chào mừng trở lại, quản trị viên</p>
        </div>

        {loading ? (
          <div className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="h-32 bg-white/5 animate-pulse rounded-2xl"></div>
              ))}
            </div>
            <div className="h-[400px] bg-white/5 animate-pulse rounded-2xl"></div>
          </div>
        ) : error ? (
          <EmptyState 
            type="error"
            title="Lỗi tải dữ liệu"
            description={error}
            action={{ label: 'Thử lại ngay', onClick: () => window.location.reload() }}
          />
        ) : isEmpty ? (
          <div className="space-y-8">
             <StatsGrid />
             <Card title="Danh sách công việc" subtitle="Quản lý các tiến trình tải video">
               <EmptyState 
                 title="Chưa có Job nào"
                 description="Bạn chưa tạo tiến trình tải video nào trong ngày hôm nay."
                 action={{ label: 'Tạo Job mới', onClick: () => setIsEmpty(false) }}
               />
             </Card>
          </div>
        ) : (
          <>
            <StatsGrid />
            
            <Card title="Quản lý tiến trình" subtitle="Danh sách các video đang được xử lý trong hệ thống">
              <FilterBar />
              <DataTable />
              <Pagination currentPage={1} totalPages={12} onPageChange={() => {}} />
            </Card>
          </>
        )}
      </div>
    </AdminLayout>
  );
};

export default AdminDashboard;
