import React from 'react';
import type { InputHTMLAttributes } from 'react';
import { Input } from '../atoms/Input';

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  errorText?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  errorText,
  id,
  ...props
}) => {
  return (
    <div className="mb-4">
      <label htmlFor={id} className="block text-xs font-semibold uppercase tracking-wider text-dark-muted mb-1.5">
        {label}
      </label>
      <Input id={id} error={!!errorText} {...props} />
      {errorText && (
        <span className="block mt-1 text-xs text-accent-rose">
          {errorText}
        </span>
      )}
    </div>
  );
};
