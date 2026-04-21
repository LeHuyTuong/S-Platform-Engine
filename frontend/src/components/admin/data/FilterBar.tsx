import React from 'react';
import { Filter, RotateCcw, ListFilter } from 'lucide-react';
import { Button } from '../../common/Button';

export const FilterBar: React.FC = () => {
  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 mb-6">
      <div className="flex items-center gap-3 w-full sm:w-auto">
        {/* Type Filter */}
        <div className="relative group">
          <ListFilter size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted pointer-events-none" />
          <select className="appearance-none h-10 bg-white/5 border border-white/10 rounded-xl pl-9 pr-8 text-xs font-bold text-text hover:border-primary/50 transition-colors focus:outline-none">
            <option>Tất cả định dạng</option>
            <option>MP4 Video</option>
            <option>MP3 Audio</option>
          </select>
          <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-muted">
            <Filter size={12} />
          </div>
        </div>

        {/* Status Filter */}
        <select className="h-10 bg-white/5 border border-white/10 rounded-xl px-4 text-xs font-bold text-text hover:border-primary/50 transition-colors focus:outline-none">
          <option>Mọi trạng thái</option>
          <option>Hoàn thành</option>
          <option>Đang chạy</option>
          <option>Lỗi</option>
        </select>
      </div>

      <div className="flex items-center gap-2">
        <Button variant="ghost" size="sm">
          <RotateCcw size={14} className="mr-2" /> Làm mới
        </Button>
        <Button variant="primary" size="sm">
          + Tạo Job mới
        </Button>
      </div>
    </div>
  );
};
