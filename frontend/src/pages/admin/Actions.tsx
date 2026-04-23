import React from 'react';
import { Card } from '../../components/admin/atoms/Card';
import { Wrench, RefreshCw } from 'lucide-react';
import { backfillAdminJobTitles } from '../../api/admin';
import { toApiClientError } from '../../api/types';

const Actions: React.FC = () => {
  const [backfillingTitles, setBackfillingTitles] = React.useState(false);
  const [feedback, setFeedback] = React.useState<{ type: 'success' | 'error'; message: string } | null>(null);

  async function handleBackfillTitles() {
    setBackfillingTitles(true);
    setFeedback(null);

    try {
      const response = await backfillAdminJobTitles();
      setFeedback({
        type: 'success',
        message: `Đã backfill thành công ${response.data.updated} jobs.`,
      });
    } catch (error) {
      setFeedback({
        type: 'error',
        message: toApiClientError(error).message,
      });
    } finally {
      setBackfillingTitles(false);
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-black tracking-tight text-white">Tác vụ hệ thống</h1>
      <p className="text-sm text-slate-400">Các công cụ bảo trì và xử lý dữ liệu hàng loạt.</p>

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

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <Card
          title="Backfill Tiêu đề"
          subtitle="Cập nhật tiêu đề video cho các job cũ bị thiếu thông tin."
        >
          <div className="flex flex-col gap-4">
            <p className="text-xs text-slate-400">
              Tiến trình này sẽ quét qua toàn bộ database và tự động bổ sung tiêu đề video từ `external_item_id` nếu tiêu đề đang trống.
            </p>
            <button
              onClick={() => void handleBackfillTitles()}
              disabled={backfillingTitles}
              className="flex w-fit items-center gap-2 rounded-xl bg-sky-500 px-4 py-2 text-sm font-bold text-white transition-all hover:bg-sky-400 disabled:opacity-50"
            >
              {backfillingTitles ? (
                <RefreshCw size={16} className="animate-spin" />
              ) : (
                <Wrench size={16} />
              )}
              {backfillingTitles ? 'Đang xử lý...' : 'Chạy Backfill ngay'}
            </button>
          </div>
        </Card>

        <Card
          title="Dọn dẹp Cache"
          subtitle="Xóa các file tạm và cache hệ thống (Tính năng sắp ra mắt)."
        >
          <div className="flex flex-col gap-4">
            <p className="text-xs text-slate-400">
              Giải phóng dung lượng bộ nhớ tạm để tối ưu hiệu năng.
            </p>
            <button
              disabled
              className="flex w-fit items-center gap-2 rounded-xl bg-white/5 px-4 py-2 text-sm font-bold text-slate-500 transition-all cursor-not-allowed"
            >
              Chưa khả dụng
            </button>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Actions;
