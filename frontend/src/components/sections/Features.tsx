import React from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { Download, Globe, Shield, Zap, Layout, Monitor } from 'lucide-react';
import { motion } from 'framer-motion';

const featureData = [
  {
    icon: Download,
    title: 'Tải hàng loạt',
    description: 'Tải toàn bộ playlist, kênh hoặc nhiều URL cùng một lúc theo pipeline rõ ràng.',
    color: 'text-primary',
    bg: 'bg-primary/10',
  },
  {
    icon: Zap,
    title: 'Tốc độ ổn định',
    description: 'Tối ưu luồng xử lý và hàng đợi để theo dõi tiến trình mượt, không mù trạng thái.',
    color: 'text-success',
    bg: 'bg-success/10',
  },
  {
    icon: Monitor,
    title: 'Hỗ trợ 4K/8K',
    description: 'Giữ lựa chọn chất lượng đầu ra rõ ràng cho video gốc có độ phân giải cao.',
    color: 'text-secondary',
    bg: 'bg-secondary/10',
  },
  {
    icon: Globe,
    title: 'Đa nền tảng',
    description: 'Hỗ trợ các nguồn phổ biến như YouTube, TikTok, Facebook và Instagram.',
    color: 'text-warning',
    bg: 'bg-warning/10',
  },
  {
    icon: Shield,
    title: 'Bảo mật rõ ràng',
    description: 'Session, CSRF và phân quyền được phản ánh trực tiếp trong giao diện mới.',
    color: 'text-blue-400',
    bg: 'bg-blue-400/10',
  },
  {
    icon: Layout,
    title: 'Định dạng linh hoạt',
    description: 'Dễ dàng chọn MP4, MKV, MP3 hoặc M4A theo đúng contract backend đang dùng.',
    color: 'text-purple-400',
    bg: 'bg-purple-400/10',
  },
];

export const Features: React.FC = () => {
  return (
    <section id="features" className="bg-white/[0.02] py-24 sm:py-32">
      <Container>
        <SectionHeader
          label="Tính năng nổi bật"
          title="Mọi thứ bạn cần để tải video chuyên nghiệp"
          description="S-Platform cung cấp bộ công cụ rõ ràng để gửi yêu cầu nguồn, theo dõi job và tải file đầu ra."
        />

        <div className="grid grid-cols-1 gap-6 sm:gap-8 md:grid-cols-2 lg:grid-cols-3">
          {featureData.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="group rounded-[32px] border border-white/5 bg-card-bg p-8 transition-all hover:border-primary/20 hover:bg-white/[0.04]"
            >
              <div
                className={`mb-6 flex h-14 w-14 items-center justify-center rounded-2xl ${feature.bg} transition-transform group-hover:scale-110`}
              >
                <feature.icon className={`h-7 w-7 ${feature.color}`} />
              </div>
              <h3 className="mb-4 text-xl font-bold text-text">{feature.title}</h3>
              <p className="text-sm leading-relaxed font-medium text-muted">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </Container>
    </section>
  );
};
