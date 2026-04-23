import { Navigate, Route, Routes } from 'react-router-dom';
import Home from './pages/Home';
import DownloaderWorkspacePage from './pages/DownloaderWorkspacePage';
import AdminDashboard from './pages/AdminDashboard';

import Overview from './pages/admin/Overview';
import Jobs from './pages/admin/Jobs';
import Users from './pages/admin/Users';
import Settings from './pages/admin/Settings';
import Actions from './pages/admin/Actions';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/app/downloader" element={<DownloaderWorkspacePage />} />
      
      {/* Admin Routes */}
      <Route path="/app/admin" element={<AdminDashboard />}>
        <Route index element={<Overview />} />
        <Route path="jobs" element={<Jobs />} />
        <Route path="users" element={<Users />} />
        <Route path="settings" element={<Settings />} />
        <Route path="actions" element={<Actions />} />
      </Route>

      {/* Redirects */}
      <Route path="/admin" element={<Navigate replace to="/app/admin" />} />
      <Route path="*" element={<Navigate replace to="/" />} />
    </Routes>
  );
}

export default App;
