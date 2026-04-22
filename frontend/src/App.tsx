import { Navigate, Route, Routes } from 'react-router-dom';
import Home from './pages/Home';
import DownloaderWorkspacePage from './pages/DownloaderWorkspacePage';
import AdminDashboard from './pages/AdminDashboard';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/admin" element={<AdminDashboard />} />
      <Route path="/app/downloader" element={<DownloaderWorkspacePage />} />
      <Route path="/app/admin" element={<AdminDashboard />} />
      <Route path="*" element={<Navigate replace to="/" />} />
    </Routes>
  );
}

export default App;
