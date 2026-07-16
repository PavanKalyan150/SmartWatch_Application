import React from 'react';
import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';

interface NavItemProps {
  to: string;
  icon: ReactNode;
  label: string;
}

export const NavItem: React.FC<NavItemProps> = ({ to, icon, label }) => {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all duration-200 ${
          isActive
            ? 'bg-primary/15 text-primary border border-primary/20 shadow-md'
            : 'text-dark-muted hover:text-dark-text hover:bg-white/5 border border-transparent'
        }`
      }
    >
      {icon}
      <span>{label}</span>
    </NavLink>
  );
};
