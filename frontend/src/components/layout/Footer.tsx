import React from 'react';
import { Container } from '../common/Container';
import { Download, Video, Music, Globe, Code } from 'lucide-react';

export const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear();

  const sections = [
    {
      title: 'Sản phẩm',
      links: [
        { label: 'Tính năng', href: '#' },
        { label: 'Hướng dẫn', href: '#' },
        { label: 'API cho lập trình viên', href: '#' },
        { label: 'Lộ trình', href: '#' },
      ],
    },
    {
      title: 'Công ty',
      links: [
        { label: 'Về chúng tôi', href: '#' },
        { label: 'Blog', href: '#' },
        { label: 'Tuyển dụng', href: '#' },
        { label: 'Liên hệ', href: '#' },
      ],
    },
    {
      title: 'Pháp lý',
      links: [
        { label: 'Điều khoản', href: '#' },
        { label: 'Bảo mật', href: '#' },
        { label: 'Chính sách cookie', href: '#' },
      ],
    },
  ];

  return (
    <footer className="border-t border-white/5 bg-bg pb-10 pt-20">
      <Container>
        <div className="mb-16 grid grid-cols-2 gap-12 md:grid-cols-6 lg:grid-cols-12 lg:gap-8">
          <div className="col-span-2 md:col-span-3 lg:col-span-4">
            <div className="mb-6 flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-gradient">
                <Download className="h-5 w-5 text-white" />
              </div>
              <span className="text-xl font-black text-text">S-Platform</span>
            </div>
            <p className="mb-6 max-w-xs text-sm leading-relaxed font-medium text-muted">
              Nền tảng tải video đa nền tảng chuyên nghiệp, an toàn và nhanh chóng.
              Hỗ trợ theo dõi tiến trình, quản lý job và tải file đầu ra rõ ràng.
            </p>
            <div className="flex gap-4">
              {[Video, Music, Globe, Code].map((Icon, i) => (
                <a
                  key={i}
                  href="#"
                  className="rounded-lg bg-white/5 p-2 text-muted transition-all hover:bg-white/10 hover:text-text"
                >
                  <Icon size={18} />
                </a>
              ))}
            </div>
          </div>

          {sections.map((section) => (
            <div key={section.title} className="col-span-1 md:col-span-1 lg:col-span-2">
              <h4 className="mb-6 text-sm font-black tracking-widest text-text uppercase">{section.title}</h4>
              <ul className="space-y-4">
                {section.links.map((link) => (
                  <li key={link.label}>
                    <a href={link.href} className="text-sm font-medium text-muted transition-colors hover:text-primary">
                      {link.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="flex flex-col items-center justify-between gap-4 border-t border-white/5 pt-8 md:flex-row">
          <p className="text-xs font-medium text-muted">
            © {currentYear} S-Platform. Đã đăng ký mọi quyền. Xây dựng cho người làm nội dung số.
          </p>
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-success"></span>
            <span className="text-[10px] font-bold tracking-widest text-muted uppercase">
              Hệ thống đang hoạt động ổn định
            </span>
          </div>
        </div>
      </Container>
    </footer>
  );
};
