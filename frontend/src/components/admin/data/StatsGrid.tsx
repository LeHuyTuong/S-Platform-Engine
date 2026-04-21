import React from 'react';
import { Download, Zap, AlertCircle, Clock } from 'lucide-react';
import { Card } from '../atoms/Card';

const stats = [
  { label: 'Tổng số tải', value: '1,284,592', change: '+12.5%', icon: Download, color: 'text-primary' },
  { label: 'Đang tải', value: '42', change: 'Đang chạy', icon: Zap, color: 'text-success' },
  { label: 'Tỷ lệ lỗi', value: '0.82%', change: '-2.1%', icon: AlertCircle, color: 'text-danger' },
  { label: 'Thời gian TB', value: '45s', change: '+1s', icon: Clock, color: 'text-warning' },
];

export const StatsGrid: React.FC = () => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      {stats.map((stat) => (
        <Card key={stat.label} className="p-0">
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className={`p-2.5 rounded-xl bg-white/5 ${stat.color}`}>
                <stat.icon size={22} />
              </div>
              <span className={`text-[10px] font-black px-2 py-1 rounded-md bg-white/5 ${
                stat.change.startsWith('+') ? 'text-success' : 'text-muted'
              }`}>
                {stat.change}
              </span>
            </div>
            <h3 className="text-2xl font-black text-text mb-1 tracking-tight">{stat.value}</h3>
            <p className="text-[11px] font-bold text-muted uppercase tracking-[0.15em]">{stat.label}</p>
          </div>
          <div className="h-1 w-full bg-white/5">
            <div 
              className={`h-full bg-current ${stat.color} opacity-30`} 
              style={{ width: '65%' }}
            />
          </div>
        </Card>
      ))}
    </div>
  );
};
