import React from 'react';
import { Search, Bell, User, LayoutGrid } from 'lucide-react';

interface TopbarProps {
  sidebarWidth: number;
}

export const Topbar: React.FC<TopbarProps> = ({ sidebarWidth }) => {
  return (
    <header
      className="fixed right-0 top-0 z-30 flex h-16 items-center justify-between border-b border-white/5 bg-[#0b1020]/80 px-6 backdrop-blur-xl transition-all duration-300"
      style={{ left: sidebarWidth }}
    >
      <div className="relative w-[340px] max-w-full">
        <Search
          size={16}
          className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-500"
        />
        <input
          type="text"
          placeholder="Tìm kiếm nhanh..."
          className="h-11 w-full rounded-xl border border-white/10 bg-white/5 pl-11 pr-4 text-sm text-white outline-none placeholder:text-slate-500 focus:border-violet-500/50"
        />
      </div>

      <div className="flex items-center gap-2">
        <button className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 transition-colors hover:bg-white/5 hover:text-white">
          <LayoutGrid size={18} />
        </button>

        <button className="relative flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 transition-colors hover:bg-white/5 hover:text-white">
          <Bell size={18} />
          <span className="absolute right-2.5 top-2.5 h-2 w-2 rounded-full bg-rose-500" />
        </button>

        <div className="mx-2 h-6 w-px bg-white/5" />

        <button className="flex items-center gap-3 rounded-full p-1 transition-colors hover:bg-white/5">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-violet-500 to-fuchsia-500">
            <div className="flex h-[30px] w-[30px] items-center justify-center rounded-full bg-[#0b1020]">
              <User size={16} className="text-slate-400" />
            </div>
          </div>

          <div className="hidden text-left lg:block">
            <p className="text-[12px] font-semibold text-white">Admin</p>
            <p className="text-[10px] uppercase tracking-[0.16em] text-slate-500">
              Super Admin
            </p>
          </div>
        </button>
      </div>
    </header>
  );
};