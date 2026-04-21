import React, { useState } from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { ChevronDown } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const faqData = [
  {
    question: 'Tôi có thể tải video từ những trang web nào?',
    answer: 'S-Platform hỗ trợ hơn 1000+ website bao gồm các mạng xã hội phổ biến nhất như YouTube, TikTok (có/không có watermark), Facebook, Instagram, Twitter, Twitch, SoundCloud và nhiều trang tin tức khác.',
  },
  {
    question: 'S-Platform có miễn phí không?',
    answer: 'Có, phiên bản web hiện tại hoàn toàn miễn phí cho người dùng cá nhân. Chúng tôi không yêu cầu bạn phải đăng ký tài khoản hay trả bất kỳ khoản phí nào để tải các video tiêu chuẩn.',
  },
  {
    question: 'Làm thế nào để tải video ở chất lượng 4K?',
    answer: 'Hệ thống sẽ tự động quét các định dạng có sẵn của URL bạn cung cấp. Nếu video gốc hỗ trợ 4K/8K, bạn sẽ thấy tùy chọn tương ứng trong danh sách chọn chất lượng trước khi nhấn nút "Tải về".',
  },
  {
    question: 'Tôi có thể tải nhiều video cùng lúc không?',
    answer: 'Tính năng tải hàng loạt (Batch Download) hiện khả dụng cho Playlist YouTube hoặc thông qua việc dán danh sách URL trong giao diện ứng dụng. Hệ thống sẽ xếp chúng vào hàng đợi và xử lý lần lượt.',
  },
  {
    question: 'Mọi thông tin tải xuống có được bảo mật không?',
    answer: 'Tuyệt đối an toàn. Chúng tôi không lưu trữ nội dung video của bạn dài hạn và không theo dõi lịch sử tải xuống cá nhân. Hệ thống tự động xóa dữ liệu sau 30 phút để bảo vệ quyền riêng tư.',
  },
];

export const FAQ: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <section id="faq" className="py-24 sm:py-32 bg-white/[0.01]">
      <Container>
        <SectionHeader 
          label="Giải đáp thắc mắc" 
          title="Những câu hỏi thường gặp"
          description="Tìm câu trả lời nhanh nhất cho các thắc mắc phổ biến về dịch vụ của chúng tôi."
        />

        <div className="max-w-3xl mx-auto space-y-4">
          {faqData.map((item, index) => {
            const isOpen = openIndex === index;
            return (
              <div 
                key={index}
                className={`border border-white/5 rounded-2xl overflow-hidden transition-all duration-300 ${
                  isOpen ? 'bg-white/[0.04] border-primary/20' : 'bg-white/[0.02] hover:bg-white/[0.04]'
                }`}
              >
                <button
                  onClick={() => setOpenIndex(isOpen ? null : index)}
                  className="flex items-center justify-between w-full p-6 text-left"
                  aria-expanded={isOpen}
                >
                  <span className="text-base sm:text-lg font-bold text-text pr-8">
                    {item.question}
                  </span>
                  <div className={`transition-transform duration-300 ${isOpen ? 'rotate-180 text-primary' : 'text-muted'}`}>
                    <ChevronDown size={20} />
                  </div>
                </button>

                <AnimatePresence>
                  {isOpen && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.3, ease: 'easeInOut' }}
                    >
                      <div className="px-6 pb-6 pt-0">
                        <div className="h-[1px] w-full bg-white/5 mb-6" />
                        <p className="text-sm sm:text-base text-muted font-medium leading-relaxed">
                          {item.answer}
                        </p>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            );
          })}
        </div>
      </Container>
    </section>
  );
};
