import React from 'react';
import { Container } from '../common/Container';
import { Button } from '../common/Button';
import { Play, ShieldCheck, Zap, ArrowRight } from 'lucide-react';
import { motion } from 'framer-motion';

export const Hero: React.FC = () => {
  return (
    <section className="relative pt-40 pb-20 overflow-hidden">
      {/* Background Glows */}
      <div className="absolute top-[-10%] right-[-10%] w-[50%] h-[50%] bg-primary/20 blur-[120px] rounded-full"></div>
      <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-secondary/10 blur-[100px] rounded-full"></div>

      <Container className="relative z-10">
        <div className="flex flex-col items-center text-center">
          {/* Badge */}
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/5 border border-white/10 mb-8"
          >
            <span className="w-2 h-2 rounded-full bg-success animate-pulse"></span>
            <span className="text-[11px] font-bold text-muted uppercase tracking-widest">Version 2.4 đã sẵn sàng</span>
          </motion.div>

          {/* Title */}
          <motion.h1 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-4xl sm:text-6xl md:text-7xl lg:text-8xl font-black text-text tracking-tight leading-[1.05] mb-8 max-w-5xl"
          >
            Tải video chất lượng <span className="text-transparent bg-clip-text bg-primary-gradient">Premium</span> từ mọi nơi
          </motion.h1>

          {/* Description */}
          <motion.p 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="text-base sm:text-xl text-muted font-medium max-w-2xl mb-12 leading-relaxed"
          >
            Nền tảng tải video nhanh nhất thế giới. Hỗ trợ 4K, 8K, tách Audio và Playlist hàng loạt. 
            An toàn, không quảng cáo, hoàn toàn miễn phí cho người dùng cuối.
          </motion.p>

          {/* Buttons */}
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="flex flex-col sm:flex-row items-center gap-4 mb-20"
          >
            <Button size="lg" className="w-full sm:w-auto h-14 px-10">
              Bắt đầu tải miễn phí <ArrowRight className="ml-2 w-5 h-5" />
            </Button>
            <Button variant="secondary" size="lg" className="w-full sm:w-auto h-14 px-10">
              Xem hướng dẫn <Play className="ml-2 w-4 h-4 fill-white" />
            </Button>
          </motion.div>

          {/* Trust indicators */}
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 1, delay: 0.5 }}
            className="grid grid-cols-1 sm:grid-cols-3 gap-6 sm:gap-12"
          >
            <div className="flex items-center gap-3 text-muted">
              <Zap size={20} className="text-primary" />
              <span className="text-xs font-bold uppercase tracking-widest">Tốc độ x10</span>
            </div>
            <div className="flex items-center gap-3 text-muted">
              <ShieldCheck size={20} className="text-success" />
              <span className="text-xs font-bold uppercase tracking-widest">Tuyệt đối an toàn</span>
            </div>
            <div className="flex items-center gap-3 text-muted">
              <Play size={20} className="text-warning" />
              <span className="text-xs font-bold uppercase tracking-widest">1000+ Nền tảng</span>
            </div>
          </motion.div>
        </div>
      </Container>
    </section>
  );
};
