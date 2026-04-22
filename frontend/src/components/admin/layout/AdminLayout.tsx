import React from 'react';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

interface AdminLayoutProps {
  children: React.ReactNode;
  currentUserEmail?: string | null;
  currentUserRole?: string | null;
  onLogout?: () => void;
  onRefresh?: () => void;
  refreshing?: boolean;
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({
  children,
  currentUserEmail,
  currentUserRole,
  onLogout,
  onRefresh,
  refreshing = false,
}) => {
  const [isCollapsed, setIsCollapsed] = React.useState(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('sp-admin-sidebar-collapsed') === 'true';
    }

    return false;
  });

  React.useEffect(() => {
    localStorage.setItem('sp-admin-sidebar-collapsed', String(isCollapsed));
  }, [isCollapsed]);

  const sidebarWidth = isCollapsed ? 80 : 272;

  return (
    <div className="min-h-screen bg-[#0b1020] text-white">
      <Sidebar
        isCollapsed={isCollapsed}
        onToggle={() => setIsCollapsed((previous) => !previous)}
        currentUserEmail={currentUserEmail}
        onLogout={onLogout}
      />

      <div
        className="min-h-screen transition-all duration-300"
        style={{ paddingLeft: sidebarWidth }}
      >
        <Topbar
          sidebarWidth={sidebarWidth}
          currentUserEmail={currentUserEmail}
          currentUserRole={currentUserRole}
          onLogout={onLogout}
          onRefresh={onRefresh}
          refreshing={refreshing}
        />
        <main className="min-h-screen px-6 pb-6 pt-20">{children}</main>
      </div>
    </div>
  );
};
