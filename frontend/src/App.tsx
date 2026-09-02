import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { DashboardPage } from './pages/DashboardPage'
import { ChallengePage } from './pages/ChallengePage'
import { Layout } from './components/Layout'

function Protected({ children }: { children: React.ReactNode }) { return useAuth().token ? <Layout>{children}</Layout> : <Navigate to="/login" replace /> }
export default function App() { return <Routes><Route path="/login" element={<LoginPage />} /><Route path="/register" element={<RegisterPage />} /><Route path="/dashboard" element={<Protected><DashboardPage /></Protected>} /><Route path="/challenges/:id" element={<Protected><ChallengePage /></Protected>} /><Route path="*" element={<Navigate to="/dashboard" replace />} /></Routes> }
