import React, { useState } from 'react';
import { Container } from '../common/Container';
import { SectionHeader } from '../common/SectionHeader';
import { ChevronDown } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const faqData = [
  {
    question: 'Tôi có thể tải video từ những nguồn nào?',
    answer:
      'Frontend hiện hỗ trợ các nền tảng đã được backend map rõ như YouTube, TikTok, Facebook và Instagram.',
  },
  {
    question: 'Tại sao có yêu cầu nguồn nhưng chưa thấy job?',
    answer:
      'Với playlist hoặc profile, hệ thống cần worker phân giải nguồn trước khi tạo ra một hay nhiều job cụ thể.',
  },
  {
    question: 'Khi nào nút tải file xuất hiện?',
    answer:
      'Khi job ở trạng thái hoàn tất và endpoint `/api/v1/jobs/{id}/files` trả về ít nhất một file có `downloadUrl`.',
  },
  {
    question: 'File tải về sẽ nằm ở đâu?',
    answer:
      'Frontend chỉ kích hoạt tải xuống trong browser. Vị trí lưu file do trình duyệt quyết định, thường là thư mục Downloads mặc định.',
  },
  {
    question: 'Vì sao tôi thấy khác nhau giữa :5173 và :8080?',
    answer:
      'Trong môi trường local, `:5173` là frontend mới. Backend `:8080` hiện được cấu hình redirect về giao diện mới để tránh rơi vào màn hình cũ.',
  },
];

export const FAQ: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <section id="faq" className="bg-white/[0.01] py-24 sm:py-32">
      <Container>
        <SectionHeader
          label="Giải đáp thắc mắc"
          title="Những câu hỏi thường gặp"
          description="Các câu hỏi dưới đây tập trung vào luồng frontend mới và cách nó làm việc với backend hiện tại."
        />

        <div className="mx-auto max-w-3xl space-y-4">
          {faqData.map((item, index) => {
            const isOpen = openIndex === index;
            return (
              <div
                key={index}
                className={`overflow-hidden rounded-2xl border border-white/5 transition-all duration-300 ${
                  isOpen ? 'border-primary/20 bg-white/[0.04]' : 'bg-white/[0.02] hover:bg-white/[0.04]'
                }`}
              >
                <button
                  onClick={() => setOpenIndex(isOpen ? null : index)}
                  className="flex w-full items-center justify-between p-6 text-left"
                  aria-expanded={isOpen}
                >
                  <span className="pr-8 text-base font-bold text-text sm:text-lg">{item.question}</span>
                  <div className={`transition-transform duration-300 ${isOpen ? 'rotate-180 text-primary' : 'text-muted'}`}>
                    <ChevronDown size={20} />
                  </div>
                </button>

                <AnimatePresence>
                  {isOpen ? (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.3, ease: 'easeInOut' }}
                    >
                      <div className="px-6 pb-6 pt-0">
                        <div className="mb-6 h-[1px] w-full bg-white/5" />
                        <p className="text-sm leading-relaxed font-medium text-muted sm:text-base">{item.answer}</p>
                      </div>
                    </motion.div>
                  ) : null}
                </AnimatePresence>
              </div>
            );
          })}
        </div>
      </Container>
    </section>
  );
};
