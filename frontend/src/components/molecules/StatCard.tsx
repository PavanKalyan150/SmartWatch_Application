import React from 'react';
import type { ReactNode } from 'react';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: ReactNode;
  subtitle?: string;
  trend?: string;
  trendType?: 'up' | 'down' | 'neutral';
  colorClassName?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon,
  subtitle,
  trend,
  trendType = 'neutral',
  colorClassName = 'text-primary',
}) => {
  return (
    <div className="glass-panel glass-panel-hover p-6 rounded-xl transition-all duration-300">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-dark-muted">{title}</p>
          <h3 className="text-3xl font-extrabold tracking-tight text-white mt-2">{value}</h3>
        </div>
        <div className={`p-3 rounded-lg bg-white/5 ${colorClassName}`}>
          {icon}
        </div>
      </div>
      {(subtitle || trend) && (
        <div className="mt-4 flex items-center gap-1.5 text-xs">
          {trend && (
            <span
              className={
                trendType === 'up'
                  ? 'text-accent-emerald font-semibold'
                  : trendType === 'down'
                  ? 'text-accent-rose font-semibold'
                  : 'text-dark-muted'
              }
            >
              {trend}
            </span>
          )}
          {subtitle && <span className="text-dark-muted">{subtitle}</span>}
        </div>
      )}
    </div>
  );
};
