import React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface CardProps {
  children: React.ReactNode;
  className?: string;
  title?: string;
  subtitle?: string;
  action?: React.ReactNode;
}

export const Card: React.FC<CardProps> = ({ 
  children, 
  className,
  title,
  subtitle,
  action
}) => {
  return (
    <div className={cn(
      "bg-card-bg/20 backdrop-blur-md border border-white/5 rounded-2xl overflow-hidden",
      className
    )}>
      {(title || action) && (
        <div className="px-6 py-5 border-b border-white/5 flex items-center justify-between">
          <div>
            {title && <h3 className="text-sm font-black text-text uppercase tracking-wider">{title}</h3>}
            {subtitle && <p className="text-[11px] font-bold text-muted mt-1 uppercase tracking-widest">{subtitle}</p>}
          </div>
          {action && <div>{action}</div>}
        </div>
      )}
      <div className="p-6">
        {children}
      </div>
    </div>
  );
};
