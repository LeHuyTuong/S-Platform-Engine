import React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface SectionHeaderProps {
  label?: string;
  title: string;
  description?: string;
  centered?: boolean;
  className?: string;
}

export const SectionHeader: React.FC<SectionHeaderProps> = ({
  label,
  title,
  description,
  centered = true,
  className,
}) => {
  return (
    <div className={cn(
      "max-w-3xl mb-12 sm:mb-16",
      centered && "mx-auto text-center",
      className
    )}>
      {label && (
        <span className="inline-block px-4 py-1.5 rounded-full bg-primary/10 border border-primary/20 text-primary text-[11px] font-bold uppercase tracking-widest mb-6 animate-in fade-in slide-in-from-bottom-1">
          {label}
        </span>
      )}
      <h2 className="text-3xl sm:text-4xl md:text-5xl font-black text-text mb-6 tracking-tight leading-[1.1]">
        {title}
      </h2>
      {description && (
        <p className="text-base sm:text-lg text-muted font-medium leading-relaxed">
          {description}
        </p>
      )}
    </div>
  );
};
