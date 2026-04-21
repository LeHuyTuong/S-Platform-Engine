import React, { useState, useEffect } from 'react';
import { Container } from '../common/Container';
import { Button } from '../common/Button';
import { Menu, X, Download } from 'lucide-react';

export const Header: React.FC = () => {
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
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled ? 'bg-bg/80 backdrop-blur-xl border-b border-white/5 py-4' : 'bg-transparent py-6'
      }`}
    >
      <Container className="flex items-center justify-between">
        {/* Logo */}
        <a href="/" className="flex items-center gap-3 group">
          <div className="w-10 h-10 bg-primary-gradient rounded-xl flex items-center justify-center shadow-lg group-hover:rotate-6 transition-transform">
            <Download className="text-white w-6 h-6" />
          </div>
          <div>
            <span className="block text-lg font-black tracking-tight leading-none text-text">S-Platform</span>
            <span className="block text-[10px] uppercase tracking-widest font-bold text-muted mt-1 leading-none">Video Engine</span>
          </div>
        </a>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center gap-8">
          {navLinks.map((link) => (
            <a 
              key={link.label} 
              href={link.href} 
              className="text-sm font-bold text-muted hover:text-text transition-colors"
            >
              {link.label}
            </a>
          ))}
        </nav>

        {/* Actions */}
        <div className="hidden md:flex items-center gap-4">
          <Button variant="ghost" size="sm">Đăng nhập</Button>
          <Button size="sm">Bắt đầu ngay</Button>
        </div>

        {/* Mobile Toggle */}
        <button 
          className="md:hidden p-2 text-text"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        >
          {mobileMenuOpen ? <X /> : <Menu />}
        </button>
      </Container>

      {/* Mobile Menu */}
      {mobileMenuOpen && (
        <div className="absolute top-full left-0 right-0 bg-card-bg/95 backdrop-blur-2xl border-b border-white/5 p-6 md:hidden animate-in fade-in slide-in-from-top-4">
          <div className="flex flex-col gap-6">
            {navLinks.map((link) => (
              <a 
                key={link.label} 
                href={link.href} 
                onClick={() => setMobileMenuOpen(false)}
                className="text-lg font-bold text-text"
              >
                {link.label}
              </a>
            ))}
            <hr className="border-white/5" />
            <div className="flex flex-col gap-3">
              <Button variant="outline">Đăng nhập</Button>
              <Button>Bắt đầu ngay</Button>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};
