import React from 'react';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'primary' | 'cyan' | 'purple' | 'emerald' | 'rose' | 'muted';
}

export const Badge: React.FC<BadgeProps> = ({ children, variant = 'primary' }) => {
  const styles = {
    primary: 'bg-primary/10 text-primary border border-primary/20',
    cyan: 'bg-accent-cyan/10 text-accent-cyan border border-accent-cyan/25',
    purple: 'bg-accent-purple/10 text-accent-purple border border-accent-purple/25',
    emerald: 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/25',
    rose: 'bg-accent-rose/10 text-accent-rose border border-accent-rose/25',
    muted: 'bg-white/5 text-dark-muted border border-white/5',
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-semibold tracking-wider uppercase ${styles[variant]}`}>
      {children}
    </span>
  );
};
