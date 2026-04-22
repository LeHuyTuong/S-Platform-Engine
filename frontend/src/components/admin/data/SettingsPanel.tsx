import React from 'react';
import type { AdminSettings } from '../../../api/adminTypes';
import { Button } from '../../common/Button';
import { formatMegabytes } from '../../../features/admin/utils';

interface SettingsPanelProps {
  settings: AdminSettings | null;
  loading?: boolean;
  saving?: boolean;
  backfilling?: boolean;
  onSave: (settings: Omit<AdminSettings, 'diskUsageMb'>) => void;
  onBackfillTitles: () => void;
}

export const SettingsPanel: React.FC<SettingsPanelProps> = ({
  settings,
  loading = false,
  saving = false,
  backfilling = false,
  onSave,
  onBackfillTitles,
}) => {
  const [formValues, setFormValues] = React.useState<Omit<AdminSettings, 'diskUsageMb'>>({
    sleepInterval: 0,
    concurrentFragments: 0,
    sleepRequests: 0,
    retries: 0,
    maxFileSizeMb: 0,
  });

  React.useEffect(() => {
    if (!settings) {
      return;
    }

    setFormValues({
      sleepInterval: settings.sleepInterval,
      concurrentFragments: settings.concurrentFragments,
      sleepRequests: settings.sleepRequests,
      retries: settings.retries,
      maxFileSizeMb: settings.maxFileSizeMb,
    });
  }, [settings]);

  if (loading && !settings) {
    return <div className="text-sm text-slate-400">Đang tải cấu hình hệ thống...</div>;
  }

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <label className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Sleep interval</span>
          <input
            type="number"
            min={0}
            value={formValues.sleepInterval}
            onChange={(event) =>
              setFormValues((previous) => ({
                ...previous,
                sleepInterval: Number(event.target.value),
              }))
            }
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          />
        </label>

        <label className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Concurrent fragments</span>
          <input
            type="number"
            min={0}
            value={formValues.concurrentFragments}
            onChange={(event) =>
              setFormValues((previous) => ({
                ...previous,
                concurrentFragments: Number(event.target.value),
              }))
            }
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          />
        </label>

        <label className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Sleep requests</span>
          <input
            type="number"
            min={0}
            value={formValues.sleepRequests}
            onChange={(event) =>
              setFormValues((previous) => ({
                ...previous,
                sleepRequests: Number(event.target.value),
              }))
            }
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          />
        </label>

        <label className="space-y-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Retries</span>
          <input
            type="number"
            min={0}
            value={formValues.retries}
            onChange={(event) =>
              setFormValues((previous) => ({
                ...previous,
                retries: Number(event.target.value),
              }))
            }
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          />
        </label>

        <label className="space-y-2 md:col-span-2">
          <span className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Max file size (MB)</span>
          <input
            type="number"
            min={0}
            value={formValues.maxFileSizeMb}
            onChange={(event) =>
              setFormValues((previous) => ({
                ...previous,
                maxFileSizeMb: Number(event.target.value),
              }))
            }
            className="h-11 w-full rounded-xl border border-white/10 bg-white/5 px-4 text-sm text-white outline-none transition-colors hover:border-sky-400/40 focus:border-sky-400/40"
          />
        </label>
      </div>

      <div className="rounded-2xl border border-white/5 bg-white/[0.03] p-4 text-sm text-slate-300">
        <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-500">Dung lượng hiện tại</p>
        <p className="mt-2 text-lg font-semibold text-white">
          {settings ? formatMegabytes(settings.diskUsageMb) : 'Đang tải...'}
        </p>
      </div>

      <div id="actions" className="flex flex-col gap-3 md:flex-row">
        <Button
          variant="primary"
          size="sm"
          onClick={() => onSave(formValues)}
          disabled={saving}
        >
          {saving ? 'Đang lưu cấu hình' : 'Lưu cấu hình'}
        </Button>
        <Button
          variant="secondary"
          size="sm"
          onClick={onBackfillTitles}
          disabled={backfilling}
        >
          {backfilling ? 'Đang backfill tiêu đề' : 'Backfill tiêu đề job'}
        </Button>
      </div>
    </div>
  );
};
