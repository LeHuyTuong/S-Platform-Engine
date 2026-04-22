import React from 'react';
import {
  ChevronLeft,
  ChevronRight,
  Download,
  LayoutDashboard,
  ListOrdered,
  LogOut,
  Settings,
  Users,
  Wrench,
} from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface NavItem {
  label: string;
  icon: React.ElementType;
  href: string;
}

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
  currentUserEmail?: string | null;
  onLogout?: () => void;
}

const navigationItems: NavItem[] = [
  { label: 'Tổng quan', icon: LayoutDashboard, href: '/admin#tong-quan' },
  { label: 'Công việc', icon: ListOrdered, href: '/admin#jobs' },
  { label: 'Người dùng', icon: Users, href: '/admin#users' },
  { label: 'Cài đặt', icon: Settings, href: '/admin#settings' },
  { label: 'Tác vụ', icon: Wrench, href: '/admin#actions' },
];

const SidebarItem: React.FC<{ item: NavItem; isCollapsed: boolean }> = ({ item, isCollapsed }) => {
  const Icon = item.icon;
  const activeHash = typeof window !== 'undefined' ? window.location.hash : '';
  const itemHash = item.href.includes('#') ? `#${item.href.split('#')[1]}` : '';
  const isActive = activeHash ? activeHash === itemHash : itemHash === '#tong-quan';

  return (
    <a
      href={item.href}
      title={isCollapsed ? item.label : undefined}
      className={cn(
        'relative flex h-11 items-center overflow-hidden rounded-xl border outline-none transition-all duration-200',
        'focus-visible:ring-2 focus-visible:ring-sky-400/60',
        isCollapsed ? 'mx-auto w-11 justify-center px-0' : 'mx-2 gap-3 px-3',
        isActive
          ? 'border-sky-400/15 bg-sky-500/10 text-sky-100'
          : 'border-transparent text-slate-300/80 hover:border-white/5 hover:bg-white/[0.04] hover:text-white',
      )}
    >
      {isActive ? (
        <span className="absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-r-full bg-sky-400" />
      ) : null}

      <span
        className={cn(
          'flex h-5 w-5 shrink-0 items-center justify-center transition-colors',
          isActive ? 'text-sky-300' : 'text-slate-400',
        )}
      >
        <Icon size={18} strokeWidth={2.2} />
      </span>

      <AnimatePresence initial={false}>
        {!isCollapsed ? (
          <motion.span
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -10 }}
            className="truncate text-sm font-medium leading-none"
          >
            {item.label}
          </motion.span>
        ) : null}
      </AnimatePresence>
    </a>
  );
};

export const Sidebar: React.FC<SidebarProps> = ({
  isCollapsed,
  onToggle,
  currentUserEmail,
  onLogout,
}) => {
  return (
    <motion.aside
      initial={false}
      animate={{ width: isCollapsed ? 80 : 272 }}
      transition={{ type: 'spring', stiffness: 300, damping: 30 }}
      className="fixed left-0 top-0 z-40 flex h-screen flex-col overflow-hidden border-r border-white/[0.06] bg-[#08101f]"
    >
      <div className="pointer-events-none absolute inset-y-0 right-0 w-px bg-gradient-to-b from-transparent via-sky-400/20 to-transparent" />

      <div
        className={cn(
          'flex h-20 shrink-0 items-center overflow-hidden border-b border-white/[0.06]',
          isCollapsed ? 'justify-center px-0' : 'px-6',
        )}
      >
        <div className={cn('flex items-center', isCollapsed ? 'justify-center' : 'gap-3')}>
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-500 via-cyan-500 to-emerald-400 shadow-[0_10px_30px_rgba(14,165,233,0.32)]">
            <Download size={18} className="text-white" />
          </div>

          <AnimatePresence initial={false}>
            {!isCollapsed ? (
              <motion.div
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                className="min-w-0"
              >
                <p className="truncate text-[15px] font-extrabold uppercase tracking-tight text-white">S-Platform</p>
                <p className="mt-0.5 text-[10px] font-semibold uppercase tracking-[0.28em] text-slate-500">
                  Trung tâm quản trị
                </p>
              </motion.div>
            ) : null}
          </AnimatePresence>
        </div>
      </div>

      <div className="border-b border-white/[0.06] px-3 py-4">
        <div className={cn('rounded-2xl bg-white/[0.03] p-3', isCollapsed && 'px-0 text-center')}>
          <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-500">Admin</p>
          <AnimatePresence initial={false}>
            {!isCollapsed ? (
              <motion.p
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 4 }}
                className="mt-2 truncate text-sm font-semibold text-white"
              >
                {currentUserEmail ?? 'Đang tải phiên đăng nhập'}
              </motion.p>
            ) : null}
          </AnimatePresence>
        </div>
      </div>

      <div className="flex-1 overflow-hidden px-1 py-5">
        <nav className="space-y-1">
          {navigationItems.map((item) => (
            <SidebarItem key={item.label} item={item} isCollapsed={isCollapsed} />
          ))}
        </nav>
      </div>

      <div className="border-t border-white/[0.06] p-3">
        <button
          onClick={onToggle}
          className={cn(
            'flex h-11 w-full items-center rounded-xl border border-transparent text-slate-300/80 transition-all duration-200 hover:border-white/5 hover:bg-white/[0.04] hover:text-white',
            isCollapsed ? 'justify-center px-0' : 'gap-3 px-3',
          )}
        >
          {isCollapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
          <AnimatePresence initial={false}>
            {!isCollapsed ? (
              <motion.span
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                className="text-sm font-medium leading-none"
              >
                Thu gọn
              </motion.span>
            ) : null}
          </AnimatePresence>
        </button>

        <button
          onClick={onLogout}
          className={cn(
            'mt-1 flex h-11 w-full items-center rounded-xl border border-transparent transition-all duration-200',
            'text-rose-400/90 hover:border-rose-400/10 hover:bg-rose-500/10 hover:text-rose-300',
            isCollapsed ? 'justify-center px-0' : 'gap-3 px-3',
          )}
        >
          <LogOut size={18} />
          <AnimatePresence initial={false}>
            {!isCollapsed ? (
              <motion.span
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                className="text-sm font-medium leading-none"
              >
                Đăng xuất
              </motion.span>
            ) : null}
          </AnimatePresence>
        </button>
      </div>
    </motion.aside>
  );
};
