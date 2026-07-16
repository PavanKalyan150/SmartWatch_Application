import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import api from '../services/api';

export interface User {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  points: number;
  level: string;
  role: 'ROLE_USER' | 'ROLE_ADMIN';
  device?: {
    id: number;
    name: string;
    brand: string;
    features: string[];
  };
  activities?: Array<{
    id: number;
    stepCount: number;
    activityDate: string;
    featuresUsed: string[];
  }>;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  login: (phone: string, password?: string) => Promise<void>;
  register: (data: any) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (token) {
      fetchCurrentUser();
    } else {
      setUser(null);
      setLoading(false);
    }
  }, [token]);

  const fetchCurrentUser = async () => {
    try {
      setLoading(true);
      const response = await api.get('/user/me');
      // Format the role to make sure it includes ROLE_ prefix if backend returns USER/ADMIN
      const userData = response.data;
      if (userData && userData.role && !userData.role.startsWith('ROLE_')) {
        userData.role = `ROLE_${userData.role}`;
      }
      setUser(userData);
      setError(null);
    } catch (err: any) {
      console.error('Failed to fetch user context:', err);
      logout();
    } finally {
      setLoading(false);
    }
  };

  const login = async (phone: string, password?: string) => {
    try {
      setLoading(true);
      setError(null);
      const response = await api.post('/auth/login', { phone, password });
      const { accessToken } = response.data;
      localStorage.setItem('token', accessToken);
      setToken(accessToken);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
      setLoading(false);
      throw err;
    }
  };

  const register = async (data: any) => {
    try {
      setLoading(true);
      setError(null);
      await api.post('/auth/register', data);
      setLoading(false);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed.');
      setLoading(false);
      throw err;
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    setLoading(false);
  };

  const refreshUser = async () => {
    await fetchCurrentUser();
  };

  const clearError = () => setError(null);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        loading,
        error,
        login,
        register,
        logout,
        refreshUser,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
