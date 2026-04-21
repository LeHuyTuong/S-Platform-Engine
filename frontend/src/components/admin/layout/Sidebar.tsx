import React from 'react';
import {
  LayoutDashboard,
  ListOrdered,
  Settings,
  Shield,
  Download,
  LogOut,
  BarChart4,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface NavItem {
  label: string;
  icon: React.ElementType;
  href: string;
  active?: boolean;
}

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
}

const NAVIGATION_DATA: { group: string; items: NavItem[] }[] = [
  {
    group: 'Chính',
    items: [
      { label: 'Tổng quan', icon: LayoutDashboard, href: '#', active: true },
      { label: 'Quản lý Job', icon: ListOrdered, href: '#' },
    ],
  },
  {
    group: 'Dữ liệu',
    items: [{ label: 'Thống kê', icon: BarChart4, href: '#' }],
  },
  {
    group: 'Hệ thống',
    items: [
      { label: 'Cấu hình', icon: Settings, href: '#' },
      { label: 'Bảo mật', icon: Shield, href: '#' },
    ],
  },
];

const SidebarItem: React.FC<{ item: NavItem; isCollapsed: boolean }> = ({
  item,
  isCollapsed,
}) => {
  const Icon = item.icon;

  return (
    <a
      href={item.href}
      title={isCollapsed ? item.label : undefined}
      className={cn(
        'relative flex h-11 items-center rounded-xl border transition-all duration-200 outline-none overflow-hidden',
        'focus-visible:ring-2 focus-visible:ring-violet-400/60',
        isCollapsed ? 'justify-center px-0 mx-auto w-11' : 'gap-3 px-3 mx-2',
        item.active
          ? 'border-violet-400/10 bg-[linear-gradient(90deg,rgba(139,92,246,0.18),rgba(139,92,246,0.08))] text-violet-200 shadow-[inset_0_1px_0_rgba(255,255,255,0.03)]'
          : 'border-transparent text-slate-300/80 hover:border-white/5 hover:bg-white/[0.04] hover:text-white'
      )}
    >
      {item.active && (
        <motion.span 
          layoutId="activeIndicator"
          className="absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-r-full bg-gradient-to-b from-violet-400 to-fuchsia-500" 
        />
      )}

      <span
        className={cn(
          'flex h-5 w-5 shrink-0 items-center justify-center transition-colors',
          item.active ? 'text-violet-300' : 'text-slate-400'
        )}
      >
        <Icon size={18} strokeWidth={2.2} />
      </span>

      <AnimatePresence initial={false}>
        {!isCollapsed && (
          <motion.span 
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -10 }}
            className="truncate text-sm font-medium leading-none"
          >
            {item.label}
          </motion.span>
        )}
      </AnimatePresence>
    </a>
  );
};

export const Sidebar: React.FC<SidebarProps> = ({ isCollapsed, onToggle }) => {
  return (
    <motion.aside
      initial={false}
      animate={{ width: isCollapsed ? 80 : 272 }}
      transition={{ type: 'spring', stiffness: 300, damping: 30 }}
      className="fixed left-0 top-0 z-40 flex h-screen flex-col overflow-hidden border-r border-white/[0.06] bg-[#08101f]"
    >
      {/* subtle edge glow */}
      <div className="pointer-events-none absolute inset-y-0 right-0 w-px bg-gradient-to-b from-transparent via-violet-400/20 to-transparent" />

      {/* Brand */}
      <div
        className={cn(
          'flex h-20 shrink-0 items-center border-b border-white/[0.06] overflow-hidden',
          isCollapsed ? 'justify-center px-0' : 'px-6'
        )}
      >
        <div
          className={cn(
            'flex items-center',
            isCollapsed ? 'justify-center' : 'gap-3'
          )}
        >
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-500 via-purple-500 to-fuchsia-500 shadow-[0_10px_30px_rgba(139,92,246,0.38)]">
            <Download size={18} className="text-white" />
          </div>

          <AnimatePresence initial={false}>
            {!isCollapsed && (
              <motion.div 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                className="min-w-0"
              >
                <p className="truncate text-[15px] font-extrabold uppercase tracking-tight text-white">
                  S-Platform
                </p>
                <p className="mt-0.5 text-[10px] font-semibold uppercase tracking-[0.28em] text-slate-500">
                  Console
                </p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex-1 overflow-hidden py-5 px-1">
        <div className="space-y-6">
          {NAVIGATION_DATA.map((section) => (
            <section key={section.group}>
              <AnimatePresence initial={false}>
                {!isCollapsed && (
                  <motion.p 
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="mb-2 px-4 text-[11px] font-bold uppercase tracking-[0.18em] text-slate-500"
                  >
                    {section.group}
                  </motion.p>
                )}
              </AnimatePresence>

              <nav className="space-y-1">
                {section.items.map((item) => (
                  <SidebarItem
                    key={item.label}
                    item={item}
                    isCollapsed={isCollapsed}
                  />
                ))}
              </nav>
            </section>
          ))}
        </div>
      </div>

      {/* Bottom */}
      <div className="border-t border-white/[0.06] p-3">
        <button
          onClick={onToggle}
          className={cn(
            'flex h-11 w-full items-center rounded-xl border border-transparent text-slate-300/80 transition-all duration-200 hover:border-white/5 hover:bg-white/[0.04] hover:text-white',
            isCollapsed ? 'justify-center px-0' : 'gap-3 px-3'
          )}
        >
          {isCollapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
          <AnimatePresence initial={false}>
            {!isCollapsed && (
              <motion.span 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                className="text-sm font-medium leading-none"
              >
                Thu gọn
              </motion.span>
            )}
          </AnimatePresence>
        </button>

        <button
          className={cn(
            'mt-1 flex h-11 w-full items-center rounded-xl border border-transparent transition-all duration-200',
            'text-rose-400/90 hover:border-rose-400/10 hover:bg-rose-500/10 hover:text-rose-300',
            isCollapsed ? 'justify-center px-0' : 'gap-3 px-3'
          )}
        >
          <LogOut size={18} />
          <AnimatePresence initial={false}>
            {!isCollapsed && (
              <motion.span 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                className="text-sm font-medium leading-none"
              >
                Đăng xuất
              </motion.span>
            )}
          </AnimatePresence>
        </button>
      </div>
    </motion.aside>
  );
};