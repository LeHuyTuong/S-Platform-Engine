import { ArrowRight, Download } from 'lucide-react';
import { motion } from 'framer-motion';
import { Button } from '../common/Button';
import { Container } from '../common/Container';

export const FinalCTA = () => {
  return (
    <section className="relative overflow-hidden py-24">
      <div className="absolute left-0 top-0 h-[1px] w-full bg-gradient-to-r from-transparent via-primary/30 to-transparent" />
      <div className="pointer-events-none absolute bottom-1/2 left-1/2 h-[600px] w-[600px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/10 blur-[140px]" />

      <Container>
        <motion.div
          initial={{ opacity: 0, scale: 0.98 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="relative z-10 overflow-hidden rounded-[48px] border border-white/5 bg-card-bg px-8 py-16 text-center sm:py-24"
        >
          <div className="absolute left-1/2 top-0 h-[200px] w-[200px] -translate-x-1/2 rounded-full bg-primary/20 blur-[60px]" />

          <div className="relative z-10">
            <div className="mx-auto mb-8 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/10">
              <Download className="h-8 w-8 text-primary" />
            </div>

            <h2 className="mx-auto mb-6 max-w-2xl text-3xl leading-tight font-black tracking-tight text-text sm:text-5xl">
              Sẵn sàng chuyển từ landing page sang không gian tải video thật?
            </h2>

            <p className="mx-auto mb-10 max-w-xl text-base leading-relaxed font-medium text-muted sm:text-lg">
              Mở giao diện mới để tạo yêu cầu nguồn, theo dõi job của bạn và tải file khi pipeline hoàn tất.
            </p>

            <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Button size="lg" className="h-14 w-full px-10 sm:w-auto" href="/app/downloader">
                Mở không gian downloader <ArrowRight className="ml-2 h-5 w-5" />
              </Button>
              <Button variant="secondary" size="lg" className="h-14 w-full px-10 sm:w-auto" href="/login">
                Đăng nhập bằng backend hiện tại
              </Button>
            </div>

            <p className="mt-8 text-[11px] font-bold tracking-[0.2em] text-muted uppercase">
              Không thêm thư viện query • Không dùng dữ liệu giả • Chỉ dùng contract thật
            </p>
          </div>
        </motion.div>
      </Container>
    </section>
  );
};
