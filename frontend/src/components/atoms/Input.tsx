import React from 'react';
import type { InputHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
}

export const Input: React.FC<InputProps> = ({
  className = '',
  error = false,
  ...props
}) => {
  return (
    <input
      className={`w-full px-4 py-2 rounded-lg glass-input focus:outline-none transition-all duration-200 ${
        error
          ? 'border-accent-rose focus:border-accent-rose focus:ring-2 focus:ring-accent-rose/20'
          : 'focus:border-primary focus:ring-2 focus:ring-primary/20'
      } ${className}`}
      {...props}
    />
  );
};
