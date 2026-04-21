import React from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { Download, Globe, Shield, Zap, Layout, Monitor } from 'lucide-react';
import { motion } from 'framer-motion';

const featureData = [
  {
    icon: Download,
    title: 'Tải hàng loạt',
    description: 'Tải toàn bộ Playlist, Channel hoặc nhiều URL cùng một lúc một cách tự động.',
    color: 'text-primary',
    bg: 'bg-primary/10',
  },
  {
    icon: Zap,
    title: 'Tốc độ vô hạn',
    description: 'Sử dụng công nghệ đa luồng giúp tối ưu hóa băng thông, tải nhanh gấp 10 lần.',
    color: 'text-success',
    bg: 'bg-success/10',
  },
  {
    icon: Monitor,
    title: 'Hỗ trợ 4K/8K',
    description: 'Tải video với độ phân giải cao nhất có sẵn, giữ nguyên chất lượng gốc.',
    color: 'text-secondary',
    bg: 'bg-secondary/10',
  },
  {
    icon: Globe,
    title: '1000+ Websites',
    description: 'Hỗ trợ hầu hết các nền tảng phổ biến: YouTube, TikTok, Facebook, Instagram...',
    color: 'text-warning',
    bg: 'bg-warning/10',
  },
  {
    icon: Shield,
    title: 'An toàn & Sạch',
    description: 'Cam kết không chứa mã độc, không quảng cáo pop-up, bảo vệ quyền riêng tư người dùng.',
    color: 'text-blue-400',
    bg: 'bg-blue-400/10',
  },
  {
    icon: Layout,
    title: 'Định dạng đa dạng',
    description: 'Dễ dàng chuyển đổi sang MP4, MKV, MP3, M4A tùy theo nhu cầu sử dụng của bạn.',
    color: 'text-purple-400',
    bg: 'bg-purple-400/10',
  },
];

export const Features: React.FC = () => {
  return (
    <section id="features" className="py-24 sm:py-32 bg-white/[0.02]">
      <Container>
        <SectionHeader 
          label="Tính năng vượt trội"
          title="Mọi thứ bạn cần để tải Video chuyên nghiệp"
          description="S-Platform cung cấp bộ công cụ mạnh mẽ giúp việc lưu trữ nội dung số trở nên đơn giản hơn bao giờ hết."
        />

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
          {featureData.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="p-8 bg-card-bg border border-white/5 rounded-[32px] hover:border-primary/20 hover:bg-white/[0.04] transition-all group"
            >
              <div className={`w-14 h-14 rounded-2xl ${feature.bg} flex items-center justify-center mb-6 group-hover:scale-110 transition-transform`}>
                <feature.icon className={`w-7 h-7 ${feature.color}`} />
              </div>
              <h3 className="text-xl font-bold text-text mb-4">{feature.title}</h3>
              <p className="text-sm text-muted font-medium leading-relaxed">
                {feature.description}
              </p>
            </motion.div>
          ))}
        </div>
      </Container>
    </section>
  );
};
