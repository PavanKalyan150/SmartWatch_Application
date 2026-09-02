import { Activity, LayoutDashboard, LogOut, Trophy } from 'lucide-react'
import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../auth'
import type { ReactNode } from 'react'

export function Layout({ children }: { children: ReactNode }) {
  const { logout } = useAuth()
  return <div className="app-shell"><aside className="sidebar"><Link to="/dashboard" className="brand"><Activity /> Pulse<span>Track</span></Link><nav><NavLink to="/dashboard"><LayoutDashboard size={17} /> Dashboard</NavLink><a href="#challenges"><Trophy size={17} /> Challenges</a></nav><button className="link-button" onClick={logout}><LogOut size={17} /> Sign out</button></aside><main>{children}</main></div>
}
export function EmptyState({ message }: { message: string }) { return <div className="empty"><Trophy size={30} /><p>{message}</p></div> }
