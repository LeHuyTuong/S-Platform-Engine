import { useState, type FormEvent } from 'react';
import { ChevronDown, Loader2, Sparkles } from 'lucide-react';
import { Button } from '../../../components/common/Button';
import type {
  DownloadType,
  PlatformOption,
  SourceTypeOption,
  SubmitSourceRequestPayload,
} from '../../../api/downloaderTypes';
import type { ApiClientError } from '../../../api/types';
import {
  AUDIO_FORMAT_OPTIONS,
  DOWNLOAD_TYPE_OPTIONS,
  PLATFORM_OPTIONS,
  SOURCE_TYPE_OPTIONS,
  VIDEO_FORMAT_OPTIONS,
  VIDEO_QUALITY_OPTIONS,
} from '../constants';

interface Props {
  isSubmitting: boolean;
  submitError: ApiClientError | null;
  onSubmit: (payload: SubmitSourceRequestPayload) => Promise<void>;
}

interface FormState {
  sourceUrl: string;
  platform: PlatformOption;
  sourceType: SourceTypeOption;
  downloadType: DownloadType;
  format: string;
  quality: string;
  writeThumbnail: boolean;
  cleanMetadata: boolean;
  proxy: string;
  startTime: string;
  endTime: string;
  titleTemplate: string;
  watermarkText: string;
}

const inputClassName =
  'w-full rounded-2xl border border-white/10 bg-white/[0.03] px-4 py-3 text-sm text-text outline-none transition-colors placeholder:text-muted focus:border-primary/50';
const checkboxClassName =
  'h-4 w-4 rounded border-white/15 bg-white/5 text-primary focus:ring-2 focus:ring-primary/40';

function createDefaultValues(): FormState {
  return {
    sourceUrl: '',
    platform: 'AUTO',
    sourceType: 'AUTO',
    downloadType: 'VIDEO',
    format: 'mp4',
    quality: 'best',
    writeThumbnail: true,
    cleanMetadata: false,
    proxy: '',
    startTime: '',
    endTime: '',
    titleTemplate: '',
    watermarkText: '',
  };
}

function normalizeNullable(value: string) {
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : null;
}

export const SourceRequestForm = ({ isSubmitting, submitError, onSubmit }: Props) => {
  const [form, setForm] = useState<FormState>(createDefaultValues);
  const [clientError, setClientError] = useState<string | null>(null);

  const formatOptions = form.downloadType === 'AUDIO' ? AUDIO_FORMAT_OPTIONS : VIDEO_FORMAT_OPTIONS;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const sourceUrl = form.sourceUrl.trim();
    if (!sourceUrl) {
      setClientError('Vui lòng nhập URL cần tải.');
      return;
    }

    setClientError(null);

    try {
      await onSubmit({
        sourceUrl,
        platform: form.platform,
        sourceType: form.sourceType,
        downloadType: form.downloadType,
        quality: form.downloadType === 'AUDIO' ? 'best' : normalizeNullable(form.quality),
        format: form.format,
        writeThumbnail: form.writeThumbnail,
        cleanMetadata: form.cleanMetadata,
        startTime: normalizeNullable(form.startTime),
        endTime: normalizeNullable(form.endTime),
        proxy: normalizeNullable(form.proxy),
        titleTemplate: normalizeNullable(form.titleTemplate),
        watermarkText: normalizeNullable(form.watermarkText),
      });

      setForm(createDefaultValues());
    } catch {
      // Page container owns request state and API error mapping.
    }
  }

  const visibleError = clientError ?? submitError?.message ?? null;

  return (
    <section className="rounded-[28px] border border-white/8 bg-card-bg/95 p-6 shadow-[0_20px_80px_rgba(0,0,0,0.25)] backdrop-blur-xl">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <p className="text-[11px] font-bold tracking-[0.18em] text-primary uppercase">Tạo yêu cầu nguồn</p>
          <h2 className="mt-2 text-2xl font-black tracking-tight text-text">Tạo job từ URL thật</h2>

        </div>
        <div className="hidden rounded-2xl border border-primary/15 bg-primary/10 p-3 text-primary lg:block">
          <Sparkles size={20} />
        </div>
      </div>

      {visibleError ? (
        <div className="mb-4 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
          <div className="font-semibold">{visibleError}</div>
          {submitError?.fieldErrors?.length ? (
            <ul className="mt-2 space-y-1 text-xs text-rose-100/90">
              {submitError.fieldErrors.map((fieldError) => (
                <li key={`${fieldError.field}-${fieldError.message}`}>
                  {fieldError.field}: {fieldError.message}
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}

      <form className="space-y-5" onSubmit={handleSubmit}>
        <fieldset className="space-y-5" disabled={isSubmitting}>
          <div className="space-y-2">
            <label className="text-sm font-semibold text-text" htmlFor="source-url">
              URL nguồn
            </label>
            <input
              id="source-url"
              className={inputClassName}
              data-testid="source-url-input"
              placeholder="https://www.youtube.com/watch?v=..."
              value={form.sourceUrl}
              onChange={(event) => setForm((previous) => ({ ...previous, sourceUrl: event.target.value }))}
            />

          </div>

          <details className="group rounded-[24px] border border-white/8 bg-black/10">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-5 py-4 text-sm font-semibold text-text outline-none focus-visible:ring-2 focus-visible:ring-primary/50 rounded-[24px]">
              Tùy chọn tải & Cấu hình nâng cao
              <div className="flex items-center gap-2 text-xs text-muted">
                <ChevronDown className="h-4 w-4 transition-transform group-open:rotate-180" />
              </div>
              <ChevronDown className="h-4 w-4 hidden group-open:block transition-transform group-open:rotate-180" />
            </summary>

            <div className="space-y-6 border-t border-white/6 px-5 py-5">
              {/* Cấu hình cơ bản */}
              <div className="grid gap-4 grid-cols-2">
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="platform">
                    Nền tảng
                  </label>
                  <select
                    id="platform"
                    className={inputClassName}
                    data-testid="platform-select"
                    value={form.platform}
                    onChange={(event) =>
                      setForm((previous) => ({ ...previous, platform: event.target.value as PlatformOption }))
                    }
                  >
                    {PLATFORM_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value} className="bg-[#0d0f14] text-slate-200">
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="source-type">
                    Loại nguồn
                  </label>
                  <select
                    id="source-type"
                    className={inputClassName}
                    data-testid="source-type-select"
                    value={form.sourceType}
                    onChange={(event) =>
                      setForm((previous) => ({ ...previous, sourceType: event.target.value as SourceTypeOption }))
                    }
                  >
                    {SOURCE_TYPE_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value} className="bg-[#0d0f14] text-slate-200">
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="download-type">
                    Loại tải
                  </label>
                  <select
                    id="download-type"
                    className={inputClassName}
                    data-testid="download-type-select"
                    value={form.downloadType}
                    onChange={(event) => {
                      const nextDownloadType = event.target.value as DownloadType;

                      setForm((previous) => ({
                        ...previous,
                        downloadType: nextDownloadType,
                        format:
                          nextDownloadType === 'AUDIO'
                            ? AUDIO_FORMAT_OPTIONS.some((option) => option.value === previous.format)
                              ? previous.format
                              : 'mp3'
                            : VIDEO_FORMAT_OPTIONS.some((option) => option.value === previous.format)
                              ? previous.format
                              : 'mp4',
                        quality: nextDownloadType === 'AUDIO' ? 'best' : previous.quality,
                      }));
                    }}
                  >
                    {DOWNLOAD_TYPE_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value} className="bg-[#0d0f14] text-slate-200">
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="format">
                    Định dạng
                  </label>
                  <select
                    id="format"
                    className={inputClassName}
                    data-testid="format-select"
                    value={form.format}
                    onChange={(event) => setForm((previous) => ({ ...previous, format: event.target.value }))}
                  >
                    {formatOptions.map((option) => (
                      <option key={option.value} value={option.value} className="bg-[#0d0f14] text-slate-200">
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {form.downloadType === 'VIDEO' && (
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="quality">
                    Chất lượng ưu tiên
                  </label>
                  <select
                    id="quality"
                    className={inputClassName}
                    data-testid="quality-select"
                    value={form.quality}
                    onChange={(event) => setForm((previous) => ({ ...previous, quality: event.target.value }))}
                  >
                    {VIDEO_QUALITY_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value} className="bg-[#0d0f14] text-slate-200">
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              <hr className="border-white/5" />

              {/* Tùy chọn mở rộng */}
              <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
                <div className="space-y-2 sm:col-span-2">
                  <label className="text-sm font-semibold text-text" htmlFor="proxy">
                    Proxy
                  </label>
                  <input
                    id="proxy"
                    className={inputClassName}
                    placeholder="http://user:pass@ip:port"
                    value={form.proxy}
                    onChange={(event) => setForm((previous) => ({ ...previous, proxy: event.target.value }))}
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="start-time">
                    Bắt đầu
                  </label>
                  <input
                    id="start-time"
                    className={inputClassName}
                    placeholder="00:00:00"
                    value={form.startTime}
                    onChange={(event) => setForm((previous) => ({ ...previous, startTime: event.target.value }))}
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="end-time">
                    Kết thúc
                  </label>
                  <input
                    id="end-time"
                    className={inputClassName}
                    placeholder="00:00:30"
                    value={form.endTime}
                    onChange={(event) => setForm((previous) => ({ ...previous, endTime: event.target.value }))}
                  />
                </div>
              </div>

              <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="title-template">
                    Mẫu tiêu đề
                  </label>
                  <input
                    id="title-template"
                    className={inputClassName}
                    placeholder="{channel} - {title}"
                    value={form.titleTemplate}
                    onChange={(event) =>
                      setForm((previous) => ({ ...previous, titleTemplate: event.target.value }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-semibold text-text" htmlFor="watermark-text">
                    Nội dung watermark
                  </label>
                  <input
                    id="watermark-text"
                    className={inputClassName}
                    placeholder="@tenkenh hoặc domain"
                    value={form.watermarkText}
                    onChange={(event) =>
                      setForm((previous) => ({ ...previous, watermarkText: event.target.value }))
                    }
                  />
                </div>
              </div>

              <div className="grid gap-3 grid-cols-1 sm:grid-cols-2">
                <label className="flex items-start gap-3 rounded-2xl border border-white/8 bg-white/[0.02] px-4 py-3 cursor-pointer hover:bg-white/[0.04] transition-colors">
                  <input
                    className={checkboxClassName}
                    type="checkbox"
                    checked={form.writeThumbnail}
                    onChange={(event) =>
                      setForm((previous) => ({ ...previous, writeThumbnail: event.target.checked }))
                    }
                  />
                  <span>
                    <span className="block text-sm font-semibold text-text">Tải thumbnail</span>

                  </span>
                </label>

                <label className="flex items-start gap-3 rounded-2xl border border-white/8 bg-white/[0.02] px-4 py-3 cursor-pointer hover:bg-white/[0.04] transition-colors">
                  <input
                    className={checkboxClassName}
                    type="checkbox"
                    checked={form.cleanMetadata}
                    onChange={(event) =>
                      setForm((previous) => ({ ...previous, cleanMetadata: event.target.checked }))
                    }
                  />
                  <span>
                    <span className="block text-sm font-semibold text-text">Làm sạch metadata</span>

                  </span>
                </label>
              </div>
            </div>
          </details>
        </fieldset>

        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">

          <Button className="min-w-[220px]" data-testid="submit-source-request" disabled={isSubmitting} type="submit">
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang gửi request
              </>
            ) : (
              'Tạo yêu cầu nguồn'
            )}
          </Button>
        </div>
      </form>
    </section>
  );
};
