import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface ButtonBaseProps {
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
  size?: 'sm' | 'md' | 'lg' | 'icon';
  className?: string;
}

type NativeButtonProps = ButtonBaseProps &
  ButtonHTMLAttributes<HTMLButtonElement> & { href?: undefined };
type AnchorButtonProps = ButtonBaseProps &
  AnchorHTMLAttributes<HTMLAnchorElement> & { href: string };

type ButtonProps = NativeButtonProps | AnchorButtonProps;

const variants = {
  primary:
    'bg-primary-gradient text-white shadow-[0_4px_14px_rgba(139,92,246,0.3)] hover:translate-y-[-2px] hover:shadow-[0_6px_20px_rgba(217,70,239,0.4)]',
  secondary: 'border border-white/10 bg-white/10 text-white hover:bg-white/20',
  outline: 'border border-primary/40 bg-transparent text-white hover:border-primary/80 hover:bg-primary/10',
  ghost: 'bg-transparent text-muted hover:bg-white/5 hover:text-text',
} as const;

const sizes = {
  sm: 'px-4 py-2 text-xs',
  md: 'px-6 py-3 text-sm',
  lg: 'px-8 py-4 text-base',
  icon: 'p-3',
} as const;

function stripSharedProps(props: ButtonProps) {
  const nextProps = { ...props };
  delete nextProps.children;
  delete nextProps.variant;
  delete nextProps.size;
  delete nextProps.className;
  return nextProps;
}

export const Button = (props: ButtonProps) => {
  const variant = props.variant ?? 'primary';
  const size = props.size ?? 'md';
  const classes = cn(
    'inline-flex items-center justify-center rounded-xl font-bold transition-all duration-200 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50',
    variants[variant],
    sizes[size],
    props.className,
  );

  if ('href' in props && props.href) {
    const anchorProps = stripSharedProps(props) as AnchorHTMLAttributes<HTMLAnchorElement> & {
      href: string;
    };

    return (
      <a className={classes} {...anchorProps}>
        {props.children}
      </a>
    );
  }

  const buttonProps = stripSharedProps(props) as ButtonHTMLAttributes<HTMLButtonElement>;

  return (
    <button className={classes} type={buttonProps.type ?? 'button'} {...buttonProps}>
      {props.children}
    </button>
  );
};
