import React from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { Star, Quote } from 'lucide-react';
import { motion } from 'framer-motion';

const testimonials = [
  {
    name: 'Nguyễn Đình Văn',
    role: 'Video Editor @ VinFast',
    content: 'Tôi đã thử rất nhiều công cụ nhưng S-Platform thực sự khác biệt về tốc độ và chất lượng file tải về. Hỗ trợ 4K cực tốt.',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
  },
  {
    name: 'Lê Hoàng Minh',
    role: 'Content Creator',
    content: 'Tải Playlist toàn bộ Channel YouTube chưa bao giờ nhàn đến thế. Giao diện cực sạch và không có quảng cáo khó chịu.',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Aneka',
  },
  {
    name: 'Trần Thị Mai',
    role: 'Digital Marketing',
    content: 'Tính năng tách âm thanh MP3 từ video TikTok rất nhanh. Giúp tôi tiết kiệm hàng giờ mỗi khi cần làm tư liệu video.',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Hoppy',
  },
];

export const Testimonials: React.FC = () => {
  return (
    <section id="testimonials" className="py-24 sm:py-32 bg-white/[0.01]">
      <Container>
        <SectionHeader 
          label="Sự hài lòng từ người dùng" 
          title="Được tin dùng bởi hơn 50,000+ nhà sáng tạo"
          description="S-Platform là công cụ không thể thiếu cho những ai cần lưu trữ và tái sử dụng nội dung video chất lượng cao."
        />

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {testimonials.map((item, index) => (
            <motion.div
              key={item.name}
              initial={{ opacity: 0, scale: 0.95 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="p-8 bg-card-bg border border-white/5 rounded-[40px] relative hover:bg-white/[0.04] transition-all group"
            >
              <div className="absolute top-8 right-8 text-white/5 group-hover:text-primary/10 transition-colors">
                <Quote size={48} />
              </div>

              {/* Stars */}
              <div className="flex gap-1 mb-6">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} size={14} className="fill-warning text-warning" />
                ))}
              </div>

              <p className="text-base text-muted font-medium italic mb-8 relative z-10 leading-relaxed">
                "{item.content}"
              </p>

              <div className="flex items-center gap-4">
                <img src={item.avatar} alt={item.name} className="w-12 h-12 rounded-full border border-white/10" />
                <div>
                  <h4 className="text-sm font-bold text-text">{item.name}</h4>
                  <p className="text-[11px] font-bold text-muted uppercase tracking-wider">{item.role}</p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </Container>
    </section>
  );
};
