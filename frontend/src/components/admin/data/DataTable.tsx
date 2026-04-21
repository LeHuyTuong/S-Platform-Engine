import React from 'react';
import { Play, Pause, Trash2, ExternalLink, ChevronRight } from 'lucide-react';
import { Badge, type BadgeVariant } from '../atoms/Badge';

interface JobData {
  id: string;
  title: string;
  url: string;
  type: 'Video' | 'Audio';
  status: 'Running' | 'Completed' | 'Failed' | 'Queued';
  progress: number;
  time: string;
}

const mockData: JobData[] = [
  { id: '1', title: 'Sơn Tùng M-TP - Making My Way', url: 'youtube.com/watch?v=...', type: 'Video', status: 'Running', progress: 45, time: '2 phút trước' },
  { id: '2', title: 'Hoàng Thùy Linh - See Tình', url: 'youtube.com/watch?v=...', type: 'Audio', status: 'Completed', progress: 100, time: '15 phút trước' },
  { id: '3', title: 'Funny Cats Compilation 2024', url: 'tiktok.com/@user/v/...', type: 'Video', status: 'Failed', progress: 12, time: '1 giờ trước' },
  { id: '4', title: 'TED Talk: The power of vulnerability', url: 'ted.com/talks/...', type: 'Video', status: 'Queued', progress: 0, time: '3 giờ trước' },
  { id: '5', title: 'Chill Lo-fi Hip Hop Beats', url: 'youtube.com/live/...', type: 'Audio', status: 'Completed', progress: 100, time: '5 giờ trước' },
];

const statusConfig: Record<string, { label: string; variant: BadgeVariant }> = {
  Running: { label: 'Đang tải', variant: 'info' },
  Completed: { label: 'Xong', variant: 'success' },
  Failed: { label: 'Lỗi', variant: 'danger' },
  Queued: { label: 'Chờ', variant: 'muted' },
};

export const DataTable: React.FC = () => {
  return (
    <div className="w-full overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-white/5">
            <th className="pb-4 px-2 text-[10px] font-black text-muted uppercase tracking-[0.2em] w-12">#</th>
            <th className="pb-4 px-4 text-[10px] font-black text-muted uppercase tracking-[0.2em]">Thông tin Video</th>
            <th className="pb-4 px-4 text-[10px] font-black text-muted uppercase tracking-[0.2em]">Định dạng</th>
            <th className="pb-4 px-4 text-[10px] font-black text-muted uppercase tracking-[0.2em]">Trạng thái</th>
            <th className="pb-4 px-4 text-[10px] font-black text-muted uppercase tracking-[0.2em]">Tiến độ</th>
            <th className="pb-4 px-4 text-[10px] font-black text-muted uppercase tracking-[0.2em] text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-white/5">
          {mockData.map((job, idx) => (
            <tr key={job.id} className="group hover:bg-white/[0.02] transition-colors">
              <td className="py-5 px-2 text-xs font-bold text-muted/50">{idx + 1}</td>
              <td className="py-5 px-4 min-w-[300px]">
                <div className="flex flex-col gap-1">
                  <span className="text-sm font-bold text-text truncate max-w-xs">{job.title}</span>
                  <span className="text-[10px] font-medium text-muted flex items-center gap-1">
                    {job.url} <ExternalLink size={10} />
                  </span>
                </div>
              </td>
              <td className="py-5 px-4">
                <span className="text-xs font-bold text-text">{job.type}</span>
              </td>
              <td className="py-5 px-4">
                <Badge variant={statusConfig[job.status].variant}>
                  {statusConfig[job.status].label}
                </Badge>
              </td>
              <td className="py-5 px-4 w-40">
                <div className="flex items-center gap-3">
                  <div className="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <div 
                      className={`h-full rounded-full transition-all duration-500 ${
                        job.status === 'Failed' ? 'bg-rose-500' : 'bg-primary'
                      }`}
                      style={{ width: `${job.progress}%` }}
                    />
                  </div>
                  <span className="text-[10px] font-black text-muted w-8">{job.progress}%</span>
                </div>
              </td>
              <td className="py-5 px-4 text-right">
                <div className="flex items-center justify-end gap-1">
                  <button className="p-2 rounded-lg hover:bg-white/5 text-muted hover:text-primary transition-all">
                    {job.status === 'Running' ? <Pause size={14} /> : <Play size={14} />}
                  </button>
                  <button className="p-2 rounded-lg hover:bg-white/5 text-muted hover:text-rose-400 transition-all">
                    <Trash2 size={14} />
                  </button>
                  <button className="p-2 rounded-lg hover:bg-white/5 text-muted hover:text-text transition-all">
                    <ChevronRight size={14} />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
