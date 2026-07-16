import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Link, useNavigate } from 'react-router-dom';
import { FormField } from '../molecules/FormField';
import { Button } from '../atoms/Button';
import { UserPlus } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const { register, error, clearError } = useAuth();
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!phone || !email || !fullName || !password) return;

    try {
      setLoading(true);
      clearError();
      await register({ phone, email, fullName, password });
      setSuccess(true);
      setTimeout(() => {
        navigate('/login');
      }, 1500);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {success ? (
        <div className="text-center space-y-3 py-6">
          <div className="w-12 h-12 rounded-full bg-accent-emerald/10 border border-accent-emerald/20 flex items-center justify-center mx-auto animate-bounce">
            <span className="text-accent-emerald font-bold text-xl">✓</span>
          </div>
          <h3 className="text-lg font-bold text-white">Registration Successful!</h3>
          <p className="text-xs text-dark-muted">Redirecting you to the sign-in page...</p>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            <FormField
              label="Full Name"
              id="fullName"
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="John Doe"
              required
            />

            <FormField
              label="Email Address"
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="john@example.com"
              required
            />

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

          <Button type="submit" fullWidth disabled={loading} className="gap-2 mt-4">
            <UserPlus className="w-4 h-4" />
            {loading ? 'Creating Account...' : 'Create Account'}
          </Button>

          <p className="text-xs text-dark-muted text-center mt-4">
            Already have an account?{' '}
            <Link to="/login" className="text-primary hover:underline font-semibold" onClick={clearError}>
              Sign In
            </Link>
          </p>
        </>
      )}
    </form>
  );
};
