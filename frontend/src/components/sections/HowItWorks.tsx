import React from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { MousePointer2, Settings2, CheckCircle2 } from 'lucide-react';
import { motion } from 'framer-motion';

const steps = [
  {
    icon: MousePointer2,
    title: 'Dán liên kết',
    description: 'Sao chép URL cần tải và dán vào form yêu cầu nguồn trong workspace.',
    color: 'text-primary',
  },
  {
    icon: Settings2,
    title: 'Chọn cấu hình',
    description: 'Thiết lập nền tảng, loại nguồn, định dạng và chất lượng phù hợp với nhu cầu.',
    color: 'text-success',
  },
  {
    icon: CheckCircle2,
    title: 'Theo dõi và tải về',
    description: 'Đợi job hoàn tất rồi tải file đầu ra trực tiếp từ panel chi tiết job.',
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
          description="Toàn bộ luồng được rút gọn còn yêu cầu nguồn, xử lý nền và tải file khi job hoàn tất."
        />

        <div className="relative">
          <div className="absolute left-1/2 top-[28%] hidden h-[2px] w-[70%] -translate-x-1/2 bg-gradient-to-r from-transparent via-white/10 to-transparent lg:block"></div>

          <div className="grid grid-cols-1 gap-12 lg:grid-cols-3 lg:gap-8">
            {steps.map((step, index) => (
              <motion.div
                key={step.title}
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: index * 0.2 }}
                className="group relative flex flex-col items-center text-center"
              >
                <div className="absolute right-[15%] top-0 select-none text-[100px] leading-none font-black text-white/[0.03] lg:right-0">
                  0{index + 1}
                </div>

                <div className="relative mb-10 flex h-20 w-20 items-center justify-center rounded-full border border-white/10 bg-white/5 transition-all duration-300 group-hover:border-primary/20 group-hover:bg-primary/10">
                  <step.icon className={`h-8 w-8 ${step.color}`} />
                </div>

                <h3 className="z-10 mb-4 text-2xl font-bold text-text">{step.title}</h3>
                <p className="z-10 max-w-[280px] text-sm leading-relaxed font-medium text-muted">{step.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </Container>
    </section>
  );
};
