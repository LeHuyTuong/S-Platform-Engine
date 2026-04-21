import React from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { MousePointer2, Settings2, CheckCircle2 } from 'lucide-react';
import { motion } from 'framer-motion';

const steps = [
  {
    icon: MousePointer2,
    title: 'Dán liên kết',
    description: 'Sao chép và dán URL từ trình duyệt của bạn vào thanh tìm kiếm của hệ thống.',
    color: 'text-primary',
  },
  {
    icon: Settings2,
    title: 'Tùy chọn chất lượng',
    description: 'Chọn định dạng mong muốn (MP4/MP3) và độ phân giải từ 480p lên đến 4K.',
    color: 'text-success',
  },
  {
    icon: CheckCircle2,
    title: 'Hoàn thành tải về',
    description: 'Nhấn nút "Tải về" và hệ thống sẽ tự động xử lý, đóng gói file cho bạn.',
    color: 'text-secondary',
  },
];

export const HowItWorks: React.FC = () => {
  return (
    <section id="process" className="py-24 sm:py-32">
      <Container>
        <SectionHeader 
          label="Quy trình đơn giản" 
          title="Tải video chỉ trong 3 bước"
          description="Chúng tôi đã tối giản hóa mọi công đoạn kỹ thuật phức tạp để bạn có thể tải nội dung yêu thích chỉ trong vài giây."
        />

        <div className="relative">
          {/* Connector Line (Desktop) */}
          <div className="hidden lg:block absolute top-[28%] left-1/2 -translate-x-1/2 w-[70%] h-[2px] bg-gradient-to-r from-transparent via-white/10 to-transparent"></div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-12 lg:gap-8">
            {steps.map((step, index) => (
              <motion.div
                key={step.title}
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: index * 0.2 }}
                className="relative flex flex-col items-center text-center group"
              >
                {/* Step Number */}
                <div className="absolute top-0 right-[15%] lg:right-0 text-[100px] font-black text-white/[0.03] leading-none select-none">
                  0{index + 1}
                </div>

                <div className="relative w-20 h-20 rounded-full bg-white/5 border border-white/10 flex items-center justify-center mb-10 group-hover:bg-primary/10 group-hover:border-primary/20 transition-all duration-300">
                  <step.icon className={`w-8 h-8 ${step.color}`} />
                </div>
                
                <h3 className="text-2xl font-bold text-text mb-4 z-10">{step.title}</h3>
                <p className="text-sm text-muted font-medium leading-relaxed max-w-[280px] z-10">
                  {step.description}
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </Container>
    </section>
  );
};
