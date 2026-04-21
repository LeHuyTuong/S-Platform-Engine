import { Inbox, Loader2, AlertCircle } from 'lucide-react';
import { motion } from 'framer-motion';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export type EmptyStateType = 'default' | 'loading' | 'error';

// Use a local type definition to avoid Storybook docgen trying to import a non-existent value
type IconComponent = React.ComponentType<{ size?: number | string; color?: string; strokeWidth?: number | string; className?: string }>;

interface EmptyStateProps {
  type?: EmptyStateType;
  icon?: IconComponent;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  type = 'default',
  icon: CustomIcon,
  title,
  description,
  action,
  className,
}) => {
  
  const getIcon = () => {
    if (CustomIcon) return <CustomIcon className="w-12 h-12 text-muted/60" />;
    
    switch (type) {
      case 'loading':
        return <Loader2 className="w-12 h-12 text-primary animate-spin" />;
      case 'error':
        return <AlertCircle className="w-12 h-12 text-danger" />;
      default:
        return <Inbox className="w-12 h-12 text-muted/40" />;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: 'easeOut' }}
      className={cn(
        "flex flex-col items-center justify-center p-12 text-center",
        "bg-card-bg/30 backdrop-blur-md rounded-[32px]",
        "border-2 border-dashed border-border/40",
        "min-h-[320px] transition-all duration-300",
        className
      )}
    >
      {/* Icon Wrapper */}
      <div className={cn(
        "mb-6 p-6 rounded-full bg-white/5",
        "ring-1 ring-white/5 shadow-inner"
      )}>
        {getIcon()}
      </div>

      {/* Text Group */}
      <div className="max-w-md space-y-2 mb-8">
        <h3 className={cn(
          "text-xl font-bold tracking-tight",
          type === 'error' ? "text-danger" : "text-text"
        )}>
          {title}
        </h3>
        {description && (
          <p className="text-sm text-muted font-medium leading-relaxed">
            {description}
          </p>
        )}
      </div>

      {/* Action Button */}
      {action && type !== 'loading' && (
        <button
          onClick={action.onClick}
          className={cn(
            "px-8 py-3 rounded-xl font-bold text-sm",
            "transition-all duration-200 active:scale-95",
            type === 'error' 
              ? "bg-danger text-white shadow-[0_4px_14px_rgba(239,68,68,0.35)] hover:bg-danger/90"
              : "bg-primary-gradient text-white shadow-[0_4px_14px_rgba(139,92,246,0.3)] hover:translate-y-[-2px] hover:shadow-[0_6px_20px_rgba(217,70,239,0.4)]"
          )}
        >
          {action.label}
        </button>
      )}
    </motion.div>
  );
};
