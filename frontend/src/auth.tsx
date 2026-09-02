import { createContext, useContext, useState, type ReactNode } from 'react'
import { api } from './api'
import type { AuthResponse } from './types'

type AuthContextValue = { token: string | null; login: (email: string, password: string) => Promise<void>; register: (input: RegisterInput) => Promise<void>; logout: () => void }
export type RegisterInput = { email: string; password: string; fullName: string; phone: string; deviceId: number }
const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState(() => localStorage.getItem('smartwatch_token'))
  const setSession = (response: AuthResponse) => { localStorage.setItem('smartwatch_token', response.token); setToken(response.token) }
  return <AuthContext.Provider value={{ token, login: async (email, password) => setSession(await api<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })), register: async input => setSession(await api<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(input) })), logout: () => { localStorage.removeItem('smartwatch_token'); setToken(null) } }}>{children}</AuthContext.Provider>
}
export function useAuth() { const context = useContext(AuthContext); if (!context) throw new Error('useAuth must be inside AuthProvider'); return context }
