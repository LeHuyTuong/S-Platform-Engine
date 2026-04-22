import React from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { Star, Quote } from 'lucide-react';
import { motion } from 'framer-motion';

const testimonials = [
  {
    name: 'Nguyễn Đình Vân',
    role: 'Biên tập video @ VinFast',
    content:
      'Điểm tôi thích nhất là trạng thái job và file đầu ra rất rõ. Không còn phải đoán xem hệ thống đã xong hay chưa.',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix',
  },
  {
    name: 'Lê Hoàng Minh',
    role: 'Nhà sáng tạo nội dung',
    content:
      'Luồng playlist và URL trực tiếp tách bạch, nhìn vào yêu cầu nguồn là biết ngay yêu cầu nào đã tạo thành job.',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Aneka',
  },
  {
    name: 'Trần Thị Mai',
    role: 'Digital Marketing',
    content:
      'Việc tải file sau khi job hoàn tất giờ trực quan hơn nhiều. Banner báo file sẵn sàng giúp đội vận hành đỡ nhầm.',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=Hoppy',
  },
];

export const Testimonials: React.FC = () => {
  return (
    <section id="testimonials" className="bg-white/[0.01] py-24 sm:py-32">
      <Container>
        <SectionHeader
          label="Phản hồi từ người dùng"
          title="Được tin dùng bởi đội vận hành và người làm nội dung"
          description="Giao diện mới tập trung vào tính rõ ràng: session, quota, trạng thái job, log và file tải xuống."
        />

        <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
          {testimonials.map((item, index) => (
            <motion.div
              key={item.name}
              initial={{ opacity: 0, scale: 0.95 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              className="group relative rounded-[40px] border border-white/5 bg-card-bg p-8 transition-all hover:bg-white/[0.04]"
            >
              <div className="absolute right-8 top-8 text-white/5 transition-colors group-hover:text-primary/10">
                <Quote size={48} />
              </div>

              <div className="mb-6 flex gap-1">
                {[...Array(5)].map((_, i) => (
                  <Star key={i} size={14} className="fill-warning text-warning" />
                ))}
              </div>

              <p className="relative z-10 mb-8 text-base leading-relaxed font-medium text-muted italic">
                "{item.content}"
              </p>

              <div className="flex items-center gap-4">
                <img src={item.avatar} alt={item.name} className="h-12 w-12 rounded-full border border-white/10" />
                <div>
                  <h4 className="text-sm font-bold text-text">{item.name}</h4>
                  <p className="text-[11px] font-bold tracking-wider text-muted uppercase">{item.role}</p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </Container>
    </section>
  );
};
