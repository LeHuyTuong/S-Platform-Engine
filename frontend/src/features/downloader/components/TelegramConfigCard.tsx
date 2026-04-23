import React from 'react';
import { Send, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { Button } from '../../../components/common/Button';
import { updateRuntimeSettings, getRuntimeSettingsStatus, type RuntimeSettingsStatus } from '../../../api/integrations';

export const TelegramConfigCard: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [status, setStatus] = React.useState<RuntimeSettingsStatus | null>(null);
  const [token, setToken] = React.useState('');
  const [chatId, setChatId] = React.useState('');
  const [feedback, setFeedback] = React.useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const fetchStatus = React.useCallback(async () => {
    try {
      const response = await getRuntimeSettingsStatus();
      setStatus(response.data);
    } catch (error) {
      console.error('Failed to fetch telegram status', error);
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void fetchStatus();
  }, [fetchStatus]);

  async function handleSave() {
    if (!token && !chatId) return;
    
    setSaving(true);
    setFeedback(null);
    try {
      await updateRuntimeSettings({
        telegramBotToken: token,
        telegramChatId: chatId,
      });
      setFeedback({
        type: 'success',
        message: 'Đã lưu cấu hình Telegram thành công!',
      });
      setToken('');
      setChatId('');
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

  if (loading) {
    return (
      <div className="flex h-32 items-center justify-center rounded-[24px] border border-white/5 bg-white/[0.02]">
        <Loader2 className="h-5 w-5 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-[24px] border border-white/8 bg-card-bg/40 backdrop-blur-md">
      <div className="border-b border-white/5 bg-white/[0.02] px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-sky-500/20 text-sky-400">
            <Send className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">Thông báo Telegram</h3>
            <p className="text-[10px] text-slate-400">Nhận link tải video trực tiếp qua Bot Telegram</p>
          </div>
        </div>
      </div>

      <div className="p-6 space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-slate-500 uppercase px-1">Bot Token</label>
            <input
              type="password"
              placeholder={status?.hasTelegramToken ? '••••••••••••••••' : 'Dán token từ @BotFather'}
              value={token}
              onChange={(e) => setToken(e.target.value)}
              className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-all focus:border-sky-500/50 focus:bg-white/[0.08]"
            />
          </div>
          <div className="space-y-1.5">
            <label className="text-[11px] font-bold text-slate-500 uppercase px-1">Chat ID</label>
            <input
              type="text"
              placeholder={status?.hasTelegramChatId ? 'Đã cấu hình' : 'ID người nhận (vd: 6359...)'}
              value={chatId}
              onChange={(e) => setChatId(e.target.value)}
              className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-all focus:border-sky-500/50 focus:bg-white/[0.08]"
            />
          </div>
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
            variant="primary"
            size="sm"
            onClick={() => void handleSave()}
            disabled={saving || (!token && !chatId)}
            className="w-full sm:w-auto"
          >
            {saving ? 'Đang lưu...' : 'Cập nhật cấu hình'}
          </Button>
        </div>
      </div>

      <div className="border-t border-white/5 bg-white/[0.01] px-6 py-3">
        <div className="flex items-center gap-4 text-[10px] font-medium text-slate-500 uppercase">
          <span className="flex items-center gap-1.5">
            <div className={`h-1.5 w-1.5 rounded-full ${status?.hasTelegramToken ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-slate-700'}`} />
            Bot Token: {status?.hasTelegramToken ? 'Sẵn sàng' : 'Chưa có'}
          </span>
          <span className="flex items-center gap-1.5">
            <div className={`h-1.5 w-1.5 rounded-full ${status?.hasTelegramChatId ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.5)]' : 'bg-slate-700'}`} />
            Chat ID: {status?.hasTelegramChatId ? 'Sẵn sàng' : 'Chưa có'}
          </span>
        </div>
      </div>
    </div>
  );
};
