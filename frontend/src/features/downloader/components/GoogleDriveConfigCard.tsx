import React from 'react';
import { Cloud, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { Button } from '../../../components/common/Button';
import { updateRuntimeSettings, getRuntimeSettingsStatus, type RuntimeSettingsStatus } from '../../../api/integrations';

export const GoogleDriveConfigCard: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [status, setStatus] = React.useState<RuntimeSettingsStatus | null>(null);
  const [json, setJson] = React.useState('');
  const [folderId, setFolderId] = React.useState('');
  const [feedback, setFeedback] = React.useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const fetchStatus = React.useCallback(async () => {
    try {
      const response = await getRuntimeSettingsStatus();
      setStatus(response.data);
    } catch (error) {
      console.error('Failed to fetch drive status', error);
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void fetchStatus();
  }, [fetchStatus]);

  async function handleSave() {
    if (!json && !folderId) return;
    
    setSaving(true);
    setFeedback(null);
    try {
      await updateRuntimeSettings({
        googleDriveServiceAccountJson: json,
        googleDriveFolderId: folderId,
      });
      setFeedback({
        type: 'success',
        message: 'Đã lưu cấu hình Google Drive thành công!',
      });
      setJson('');
      setFolderId('');
      void fetchStatus();
    } catch (error: any) {
      setFeedback({
        type: 'error',
        message: error.message || 'Không thể lưu cấu hình.',
      });
    } finally {
      setSaving(false);
    }
  }

  if (loading) return null;

  return (
    <div className="overflow-hidden rounded-[24px] border border-white/8 bg-card-bg/40 backdrop-blur-md">
      <div className="border-b border-white/5 bg-white/[0.02] px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-500/20 text-emerald-400">
            <Cloud className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">Google Drive Auto-upload</h3>
            <p className="text-[10px] text-slate-400">Tự động sao lưu video vào đám mây của bạn</p>
          </div>
        </div>
      </div>

      <div className="p-6 space-y-4">
        <div className="space-y-1.5">
          <label className="text-[11px] font-bold text-slate-500 uppercase px-1">Service Account JSON</label>
          <textarea
            placeholder={status?.hasGoogleDriveServiceAccount ? 'Đã lưu cấu hình Service Account (Dán JSON mới để ghi đè)' : 'Dán nội dung file JSON của Google Service Account tại đây...'}
            value={json}
            onChange={(e) => setJson(e.target.value)}
            className="h-24 w-full resize-none rounded-xl border border-white/10 bg-white/5 p-4 text-[13px] text-white outline-none transition-all focus:border-emerald-500/50 focus:bg-white/[0.08]"
          />
        </div>

        <div className="space-y-1.5">
          <label className="text-[11px] font-bold text-slate-500 uppercase px-1">Folder ID</label>
          <input
            type="text"
            placeholder={status?.hasGoogleDriveFolderId ? 'Đã cấu hình Folder ID' : 'ID thư mục (vd: 1abc...)'}
            value={folderId}
            onChange={(e) => setFolderId(e.target.value)}
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-all focus:border-emerald-500/50 focus:bg-white/[0.08]"
          />
        </div>

        {feedback && (
          <div className={`flex items-center gap-2 rounded-xl border px-4 py-2.5 text-xs ${
            feedback.type === 'success' 
              ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400' 
              : 'border-rose-500/20 bg-rose-500/10 text-rose-400'
          }`}>
            {feedback.type === 'success' ? <CheckCircle2 className="h-4 w-4" /> : <AlertCircle className="h-4 w-4" />}
            {feedback.message}
          </div>
        )}

        <div className="pt-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => void handleSave()}
            disabled={saving || (!json && !folderId)}
            className="w-full sm:w-auto"
          >
            {saving ? 'Đang lưu...' : 'Cập nhật Google Drive'}
          </Button>
        </div>
      </div>

      <div className="border-t border-white/5 bg-white/[0.01] px-6 py-3">
        <div className="flex items-center gap-4 text-[10px] font-medium text-slate-500 uppercase">
          <span className="flex items-center gap-1.5">
            <div className={`h-1.5 w-1.5 rounded-full ${status?.hasGoogleDriveServiceAccount ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-slate-700'}`} />
            Auth: {status?.hasGoogleDriveServiceAccount ? 'Đã kết nối' : 'Chưa có'}
          </span>
          <span className="flex items-center gap-1.5">
            <div className={`h-1.5 w-1.5 rounded-full ${status?.hasGoogleDriveFolderId ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-slate-700'}`} />
            Folder: {status?.hasGoogleDriveFolderId ? 'Đã xác định' : 'Mặc định'}
          </span>
        </div>
      </div>
    </div>
  );
};
