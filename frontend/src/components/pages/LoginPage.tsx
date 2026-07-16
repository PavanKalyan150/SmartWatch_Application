import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Link, useNavigate } from 'react-router-dom';
import { FormField } from '../molecules/FormField';
import { Button } from '../atoms/Button';
import { LogIn } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login, error, clearError } = useAuth();
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!phone || !password) return;

    try {
      setLoading(true);
      clearError();
      await login(phone, password);
      navigate('/');
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-4">
        <FormField
          label="Phone Number"
          id="phone"
          type="text"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="e.g. 9876543210"
          required
        />

        <FormField
          label="Password"
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          required
        />
      </div>

      {error && <p className="text-xs text-accent-rose font-medium text-center">{error}</p>}

      <Button type="submit" fullWidth disabled={loading} className="gap-2">
        <LogIn className="w-4 h-4" />
        {loading ? 'Signing in...' : 'Sign In'}
      </Button>

      <p className="text-xs text-dark-muted text-center mt-4">
        Don't have an account?{' '}
        <Link to="/register" className="text-primary hover:underline font-semibold" onClick={clearError}>
          Create Account
        </Link>
      </p>
    </form>
  );
};
