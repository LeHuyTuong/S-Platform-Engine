import React from 'react';
import { AlertCircle, CheckCircle2, HardDrive, Users, Video, Wrench } from 'lucide-react';
import type { AdminDashboardStats } from '../../../api/adminTypes';
import { Card } from '../atoms/Card';
import { formatMegabytes } from '../../../features/admin/utils';

interface StatsGridProps {
  stats: AdminDashboardStats;
}

export const StatsGrid: React.FC<StatsGridProps> = ({ stats }) => {
  const cards = [
    {
      label: 'Tổng số job',
      value: stats.totalJobs.toLocaleString('vi-VN'),
      helper: 'Toàn hệ thống',
      icon: Video,
      accent: 'text-sky-300',
    },
    {
      label: 'Job hoàn thành',
      value: stats.completedJobs.toLocaleString('vi-VN'),
      helper: 'Đã xuất file',
      icon: CheckCircle2,
      accent: 'text-emerald-300',
    },
    {
      label: 'Job thất bại',
      value: stats.failedJobs.toLocaleString('vi-VN'),
      helper: 'Cần rà soát lại',
      icon: AlertCircle,
      accent: 'text-rose-300',
    },
    {
      label: 'Người dùng',
      value: stats.userCount.toLocaleString('vi-VN'),
      helper: 'Tài khoản đang quản lý',
      icon: Users,
      accent: 'text-amber-300',
    },
    {
      label: 'Dung lượng lưu trữ',
      value: formatMegabytes(stats.diskUsageMb),
      helper: 'Thư mục tải xuống',
      icon: HardDrive,
      accent: 'text-cyan-300',
    },
    {
      label: 'Công cụ hệ thống',
      value: `${stats.isYtDlpInstalled ? 'yt-dlp OK' : 'yt-dlp thiếu'} / ${stats.isFfmpegInstalled ? 'ffmpeg OK' : 'ffmpeg thiếu'}`,
      helper: 'Kiểm tra runtime',
      icon: Wrench,
      accent: stats.isYtDlpInstalled && stats.isFfmpegInstalled ? 'text-emerald-300' : 'text-rose-300',
    },
  ];

  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
      {cards.map((card) => (
        <Card key={card.label} className="p-0">
          <div className="p-6">
            <div className="mb-4 flex items-start justify-between gap-4">
              <div className={`rounded-2xl bg-white/5 p-3 ${card.accent}`}>
                <card.icon size={20} />
              </div>
              <span className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400">
                {card.helper}
              </span>
            </div>
            <h3 className="text-lg font-black tracking-tight text-white">{card.value}</h3>
            <p className="mt-2 text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">{card.label}</p>
          </div>
        </Card>
      ))}
    </div>
  );
};
