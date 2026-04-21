import React from 'react';
import { Container } from '../common/Container';
import { Button } from '../common/Button';
import { Download, ArrowRight } from 'lucide-react';
import { motion } from 'framer-motion';

export const FinalCTA: React.FC = () => {
  return (
    <section className="py-24 relative overflow-hidden">
      {/* Background Decor */}
      <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-primary/30 to-transparent"></div>
      <div className="absolute bottom-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary/10 blur-[140px] rounded-full pointer-events-none"></div>

      <Container>
        <motion.div 
          initial={{ opacity: 0, scale: 0.98 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="relative px-8 py-16 sm:py-24 bg-card-bg border border-white/5 rounded-[48px] overflow-hidden text-center z-10"
        >
          {/* Animated Glow in background */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[200px] h-[200px] bg-primary/20 blur-[60px] rounded-full"></div>

          <div className="relative z-10">
            <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto mb-8">
              <Download className="text-primary w-8 h-8" />
            </div>
            
            <h2 className="text-3xl sm:text-5xl font-black text-text mb-6 tracking-tight leading-tight max-w-2xl mx-auto">
              Sẵn sàng trải nghiệm sức mạnh của S-Platform?
            </h2>
            
            <p className="text-base sm:text-lg text-muted font-medium mb-10 max-w-xl mx-auto leading-relaxed">
              Bắt đầu tải những video yêu thích của bạn ngay bây giờ với tốc độ không giới hạn và hoàn toàn miễn phí.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Button size="lg" className="w-full sm:w-auto h-14 px-10">
                Bắt đầu ngay miễn phí <ArrowRight className="ml-2 w-5 h-5" />
              </Button>
              <Button variant="secondary" size="lg" className="w-full sm:w-auto h-14 px-10">
                Tìm hiểu thêm
              </Button>
            </div>

            <p className="mt-8 text-[11px] font-bold text-muted uppercase tracking-[0.2em]">
              Không cần đăng ký • Không cần thẻ tín dụng • Tải ngay
            </p>
          </div>
        </motion.div>
      </Container>
    </section>
  );
};
