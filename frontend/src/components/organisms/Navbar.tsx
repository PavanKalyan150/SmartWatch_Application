import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Badge } from '../atoms/Badge';
import { LogOut, User as UserIcon, Trophy } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="glass-panel border-b border-dark-border px-6 py-4 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <h2 className="text-xl font-extrabold tracking-tight bg-gradient-to-r from-primary via-accent-cyan to-accent-purple bg-clip-text text-transparent">
          PULSE.IQ
        </h2>
      </div>

      {user && (
        <div className="flex items-center gap-6">
          {/* Points display */}
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-primary/10 border border-primary/20">
            <Trophy className="w-4 h-4 text-primary animate-pulse" />
            <span className="text-xs font-bold text-primary">{user.points} XP</span>
          </div>

          {/* Level Badge */}
          {user.level && (
            <Badge variant={user.level === 'Champion' ? 'rose' : user.level === 'Athlete' ? 'cyan' : 'emerald'}>
              {user.level}
            </Badge>
          )}

          {/* User profile identifier */}
          <div className="flex items-center gap-3 border-l border-white/10 pl-6">
            <div className="w-8 h-8 rounded-full bg-white/5 border border-white/10 flex items-center justify-center">
              <UserIcon className="w-4 h-4 text-dark-muted" />
            </div>
            <div className="hidden sm:block text-left">
              <p className="text-xs font-semibold text-white leading-none">{user.fullName}</p>
              <p className="text-[9px] text-dark-muted leading-none mt-1 uppercase font-bold tracking-wider">
                {user.role === 'ROLE_ADMIN' ? 'Admin' : 'User'}
              </p>
            </div>

            <button
              onClick={logout}
              title="Logout"
              className="ml-3 p-1.5 rounded-lg text-dark-muted hover:text-accent-rose hover:bg-accent-rose/10 transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </header>
  );
};
