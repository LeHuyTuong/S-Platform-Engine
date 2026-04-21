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
        { label: 'API cho Dev', href: '#' },
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
        { label: 'Cookie Policy', href: '#' },
      ],
    },
  ];

  return (
    <footer className="bg-bg border-t border-white/5 pt-20 pb-10">
      <Container>
        <div className="grid grid-cols-2 md:grid-cols-6 lg:grid-cols-12 gap-12 lg:gap-8 mb-16">
          {/* Brand Info */}
          <div className="col-span-2 md:col-span-3 lg:col-span-4">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-8 h-8 bg-primary-gradient rounded-lg flex items-center justify-center">
                <Download className="text-white w-5 h-5" />
              </div>
              <span className="text-xl font-black text-text">S-Platform</span>
            </div>
            <p className="text-sm text-muted font-medium leading-relaxed mb-6 max-w-xs">
              Trình tải video đa nền tảng chuyên nghiệp, an toàn và nhanh chóng nhất. 
              Hỗ trợ hơn 1000+ website khác nhau.
            </p>
            <div className="flex gap-4">
              {[Video, Music, Globe, Code].map((Icon, i) => (
                <a key={i} href="#" className="p-2 bg-white/5 rounded-lg text-muted hover:text-text hover:bg-white/10 transition-all">
                  <Icon size={18} />
                </a>
              ))}
            </div>
          </div>

          {/* Links */}
          {sections.map((section) => (
            <div key={section.title} className="col-span-1 md:col-span-1 lg:col-span-2">
              <h4 className="text-sm font-black text-text uppercase tracking-widest mb-6">{section.title}</h4>
              <ul className="space-y-4">
                {section.links.map((link) => (
                  <li key={link.label}>
                    <a href={link.href} className="text-sm text-muted font-medium hover:text-primary transition-colors">
                      {link.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="flex flex-col md:flex-row items-center justify-between pt-8 border-t border-white/5 gap-4">
          <p className="text-xs text-muted font-medium">
            © {currentYear} S-Platform Engine. All rights reserved. Made with ❤️ for video creators.
          </p>
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-success"></span>
            <span className="text-[10px] uppercase font-bold tracking-widest text-muted">Hệ thống đang hoạt động ổn định</span>
          </div>
        </div>
      </Container>
    </footer>
  );
};
