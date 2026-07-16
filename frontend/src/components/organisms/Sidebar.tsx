import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { NavItem } from '../molecules/NavItem';
import { LayoutDashboard, Trophy, ListTodo, Cpu, ShieldAlert } from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();

  return (
    <aside className="w-64 glass-panel border-r border-dark-border min-h-[calc(100vh-73px)] p-4 flex flex-col gap-2 shrink-0">
      <p className="text-[10px] font-bold text-dark-muted px-4 py-2 uppercase tracking-widest">Navigation</p>
      
      <NavItem to="/" icon={<LayoutDashboard className="w-4 h-4" />} label="Dashboard" />
      <NavItem to="/challenges" icon={<Trophy className="w-4 h-4" />} label="Challenges" />
      <NavItem to="/tasks" icon={<ListTodo className="w-4 h-4" />} label="Tasks" />
      <NavItem to="/devices" icon={<Cpu className="w-4 h-4" />} label="Smartwatches" />

      {user?.role === 'ROLE_ADMIN' && (
        <>
          <div className="border-t border-white/5 my-4"></div>
          <p className="text-[10px] font-bold text-dark-muted px-4 py-2 uppercase tracking-widest">Admin Control</p>
          <NavItem to="/admin" icon={<ShieldAlert className="w-4 h-4 text-accent-rose" />} label="Admin Console" />
        </>
      )}
    </aside>
  );
};
