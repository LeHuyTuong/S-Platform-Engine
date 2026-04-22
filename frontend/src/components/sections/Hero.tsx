import { ArrowRight, Play, ShieldCheck, Zap } from 'lucide-react';
import { motion } from 'framer-motion';
import { Button } from '../common/Button';
import { Container } from '../common/Container';

export const Hero = () => {
  return (
    <section className="relative overflow-hidden pb-20 pt-40">
      <div className="absolute right-[-10%] top-[-10%] h-[50%] w-[50%] rounded-full bg-primary/20 blur-[120px]" />
      <div className="absolute bottom-[-10%] left-[-10%] h-[40%] w-[40%] rounded-full bg-secondary/10 blur-[100px]" />

      <Container className="relative z-10">
        <div className="flex flex-col items-center text-center">
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="mb-8 flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-1.5"
          >
            <span className="h-2 w-2 rounded-full bg-success animate-pulse" />
            <span className="text-[11px] font-bold tracking-widest text-muted uppercase">
              Phiên bản 2.4 đã sẵn sàng
            </span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="mb-8 max-w-5xl text-4xl leading-[1.05] font-black tracking-tight text-text sm:text-6xl md:text-7xl lg:text-8xl"
          >
            Tải video chất lượng <span className="bg-primary-gradient bg-clip-text text-transparent">cao cấp</span> từ mọi nơi
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="mb-12 max-w-2xl text-base leading-relaxed font-medium text-muted sm:text-xl"
          >
            Nền tảng tải video đa nguồn với tốc độ ổn định, theo dõi job thời gian thực
            và file tải xuống rõ ràng thay vì những màn hình mô phỏng.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="mb-20 flex flex-col items-center gap-4 sm:flex-row"
          >
            <Button size="lg" className="h-14 w-full px-10 sm:w-auto" href="/app/downloader">
              Mở workspace tải video <ArrowRight className="ml-2 h-5 w-5" />
            </Button>
            <Button variant="secondary" size="lg" className="h-14 w-full px-10 sm:w-auto" href="/login">
              Đăng nhập để tiếp tục <Play className="ml-2 h-4 w-4 fill-white" />
            </Button>
          </motion.div>

          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 1, delay: 0.5 }}
            className="grid grid-cols-1 gap-6 sm:grid-cols-3 sm:gap-12"
          >
            <div className="flex items-center gap-3 text-muted">
              <Zap size={20} className="text-primary" />
              <span className="text-xs font-bold tracking-widest uppercase">Theo dõi tiến độ rõ ràng</span>
            </div>
            <div className="flex items-center gap-3 text-muted">
              <ShieldCheck size={20} className="text-success" />
              <span className="text-xs font-bold tracking-widest uppercase">Session và CSRF đúng chuẩn</span>
            </div>
            <div className="flex items-center gap-3 text-muted">
              <Play size={20} className="text-warning" />
              <span className="text-xs font-bold tracking-widest uppercase">Job, log và file thật</span>
            </div>
          </motion.div>
        </div>
      </Container>
    </section>
  );
};
