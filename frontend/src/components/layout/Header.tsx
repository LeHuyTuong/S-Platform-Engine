import { useEffect, useState } from 'react';
import { Download, Menu, X } from 'lucide-react';
import { Button } from '../common/Button';
import { Container } from '../common/Container';

export const Header = () => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 20);

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { label: 'Tính năng', href: '#features' },
    { label: 'Cách hoạt động', href: '#process' },
    { label: 'Đánh giá', href: '#testimonials' },
    { label: 'Câu hỏi thường gặp', href: '#faq' },
  ];

  return (
    <header
      className={`fixed left-0 right-0 top-0 z-50 transition-all duration-300 ${
        isScrolled ? 'border-b border-white/5 bg-bg/80 py-4 backdrop-blur-xl' : 'bg-transparent py-6'
      }`}
    >
      <Container className="flex items-center justify-between">
        <a href="/" className="group flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-gradient shadow-lg transition-transform group-hover:rotate-6">
            <Download className="h-6 w-6 text-white" />
          </div>
          <div>
            <span className="block text-lg leading-none font-black tracking-tight text-text">S-Platform</span>
            <span className="mt-1 block text-[10px] leading-none font-bold tracking-widest text-muted uppercase">
              Nền tảng tải video
            </span>
          </div>
        </a>

        <nav className="hidden items-center gap-8 md:flex">
          {navLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              className="text-sm font-bold text-muted transition-colors hover:text-text"
            >
              {link.label}
            </a>
          ))}
        </nav>

        <div className="hidden items-center gap-4 md:flex">
          <Button variant="ghost" size="sm" href="/login">
            Đăng nhập
          </Button>
          <Button size="sm" href="/app/downloader">
            Bắt đầu ngay
          </Button>
        </div>

        <button
          className="p-2 text-text md:hidden"
          type="button"
          onClick={() => setMobileMenuOpen((prev) => !prev)}
        >
          {mobileMenuOpen ? <X /> : <Menu />}
        </button>
      </Container>

      {mobileMenuOpen && (
        <div className="absolute left-0 right-0 top-full animate-in fade-in slide-in-from-top-4 border-b border-white/5 bg-card-bg/95 p-6 backdrop-blur-2xl md:hidden">
          <div className="flex flex-col gap-6">
            {navLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                className="text-lg font-bold text-text"
                onClick={() => setMobileMenuOpen(false)}
              >
                {link.label}
              </a>
            ))}

            <hr className="border-white/5" />

            <div className="flex flex-col gap-3">
              <Button variant="outline" href="/login">
                Đăng nhập
              </Button>
              <Button href="/app/downloader">Bắt đầu ngay</Button>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};
