import React from 'react';
import { Card } from '../../components/admin/atoms/Card';
import { SettingsPanel } from '../../components/admin/data/SettingsPanel';
import { useAdminSettings } from '../../features/admin/hooks/useAdminSettings';
import { updateAdminSettings } from '../../api/admin';
import { toApiClientError } from '../../api/types';
import type { AdminSettings as AdminSettingsType } from '../../api/adminTypes';

const Settings: React.FC = () => {
  const settings = useAdminSettings({ enabled: true });
  const [savingSettings, setSavingSettings] = React.useState(false);
  const [feedback, setFeedback] = React.useState<{ type: 'success' | 'error'; message: string } | null>(null);

  async function handleSaveSettings(nextSettings: Omit<AdminSettingsType, 'diskUsageMb'>) {
    setSavingSettings(true);
    setFeedback(null);

    try {
      await updateAdminSettings(nextSettings);
      setFeedback({
        type: 'success',
        message: 'Đã lưu cấu hình quản trị.',
      });
      void settings.refetch();
    } catch (error) {
      setFeedback({
        type: 'error',
        message: toApiClientError(error).message,
      });
    } finally {
      setSavingSettings(false);
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-black tracking-tight text-white">Cài đặt hệ thống</h1>
      <p className="text-sm text-slate-400">Điều chỉnh các tham số runtime và cấu hình backend.</p>

      {feedback ? (
        <div
          className={`rounded-2xl border px-4 py-3 text-sm ${
            feedback.type === 'success'
              ? 'border-emerald-400/20 bg-emerald-500/10 text-emerald-200'
              : 'border-rose-400/20 bg-rose-500/10 text-rose-200'
          }`}
        >
          {feedback.message}
        </div>
      ) : null}

      <div className="max-w-3xl">
        <Card
          title="Cài đặt runtime"
          subtitle="Đọc và lưu qua `/api/v1/admin/settings`."
        >
          <SettingsPanel
            settings={settings.settings}
            loading={settings.loading}
            saving={savingSettings}
            backfilling={false}
            onSave={(nextSettings) => {
              void handleSaveSettings(nextSettings);
            }}
            onBackfillTitles={() => {}}
          />
        </Card>
      </div>
    </div>
  );
};

export default Settings;
