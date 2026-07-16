import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { StatCard } from '../molecules/StatCard';
import { TelemetryModal } from '../organisms/TelemetryModal';
import { Button } from '../atoms/Button';
import { Trophy, Activity, Cpu, Calendar, Plus, ChevronRight, BarChart3 } from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const [showTelemetryModal, setShowTelemetryModal] = useState(false);

  if (!user) return null;

  // Calculate some simple statistics
  const activitiesCount = user.activities?.length || 0;
  const recentActivities = [...(user.activities || [])]
    .sort((a, b) => new Date(b.activityDate).getTime() - new Date(a.activityDate).getTime())
    .slice(0, 5);

  return (
    <>
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-white">
            Welcome back, {user.fullName}!
          </h1>
          <p className="text-sm text-dark-muted mt-1">
            Track your fitness parameters, join challenges, and compete in the leaderboard.
          </p>
        </div>

        <Button onClick={() => setShowTelemetryModal(true)} className="gap-2 shrink-0">
          <Plus className="w-4 h-4" />
          Log Watch Activity
        </Button>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatCard
          title="Total Score"
          value={`${user.points} XP`}
          icon={<Trophy className="w-5 h-5" />}
          subtitle="Earn more by completing challenges"
          trend="+150 XP this week"
          trendType="up"
          colorClassName="text-yellow-400"
        />

        <StatCard
          title="User Tier"
          value={user.level || 'Novice'}
          icon={<Activity className="w-5 h-5" />}
          subtitle="Tier updates dynamically with points"
          trend={user.level === 'Champion' ? 'Max Rank reached' : 'Level up at next milestone'}
          trendType="neutral"
          colorClassName="text-accent-purple"
        />

        <StatCard
          title="Connected Smartwatch"
          value={user.device ? user.device.name : 'No Watch Paired'}
          icon={<Cpu className="w-5 h-5" />}
          subtitle={user.device ? `${user.device.brand} (${user.device.features.length} sensors)` : 'Pair to join compatible challenges'}
          trend={user.device ? 'Sensor stream healthy' : 'Action Required'}
          trendType={user.device ? 'up' : 'down'}
          colorClassName={user.device ? 'text-accent-cyan' : 'text-accent-rose'}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Activity Logs */}
        <div className="lg:col-span-2 glass-panel rounded-2xl p-6 border border-white/5 space-y-6">
          <div className="flex items-center justify-between">
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-accent-cyan" />
              Activity Logs (Last 30 Days)
            </h3>
            {activitiesCount > 0 && (
              <span className="text-xs text-dark-muted font-medium">{activitiesCount} logs found</span>
            )}
          </div>

          {recentActivities.length === 0 ? (
            <div className="text-center py-16">
              <Activity className="w-8 h-8 text-dark-muted mx-auto mb-3" />
              <p className="text-sm text-dark-muted">No telemetry events logged yet.</p>
              <p className="text-xs text-dark-muted/60 mt-1">Submit activity logs using your smartwatch simulator.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {recentActivities.map((act) => (
                <div
                  key={act.id}
                  className="flex items-center justify-between p-4 rounded-xl border border-white/5 bg-white/[0.01] hover:bg-white/[0.02] transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2.5 rounded-lg bg-accent-cyan/10 text-accent-cyan">
                      <Activity className="w-4 h-4" />
                    </div>
                    <div>
                      <p className="text-sm font-bold text-white">
                        {act.stepCount.toLocaleString()} steps
                      </p>
                      <div className="flex items-center gap-1.5 mt-1">
                        <Calendar className="w-3 h-3 text-dark-muted" />
                        <span className="text-[10px] text-dark-muted">
                          {new Date(act.activityDate).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Active Sensors */}
                  <div className="flex gap-1">
                    {act.featuresUsed && act.featuresUsed.map((feat: string) => (
                      <span
                        key={feat}
                        className="px-1.5 py-0.5 rounded bg-white/5 border border-white/5 text-[9px] font-semibold text-dark-muted"
                      >
                        {feat}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick Links Column */}
        <div className="glass-panel rounded-2xl p-6 border border-white/5 space-y-6">
          <h3 className="text-base font-bold text-white">Quick Actions</h3>

          <div className="flex flex-col gap-3">
            {!user.device && (
              <div className="p-4 rounded-xl border border-accent-rose/15 bg-accent-rose/5 text-xs text-accent-rose space-y-3">
                <p className="font-semibold">No Smartwatch Paired</p>
                <p className="text-[11px] leading-relaxed text-accent-rose/80">
                  Pair a smartwatch model to scan for challenges needing specific device sensors.
                </p>
                <Link
                  to="/devices"
                  className="inline-flex items-center gap-1 text-[11px] font-bold underline hover:text-rose-400"
                >
                  Link Watch Model <ChevronRight className="w-3 h-3" />
                </Link>
              </div>
            )}

            <Link
              to="/challenges"
              className="flex items-center justify-between p-4 rounded-xl border border-white/5 bg-white/[0.01] hover:bg-white/[0.03] text-sm font-semibold text-white group transition-colors"
            >
              <span>Explore Challenges</span>
              <ChevronRight className="w-4 h-4 text-dark-muted group-hover:translate-x-1 transition-transform" />
            </Link>

            <Link
              to="/tasks"
              className="flex items-center justify-between p-4 rounded-xl border border-white/5 bg-white/[0.01] hover:bg-white/[0.03] text-sm font-semibold text-white group transition-colors"
            >
              <span>Enroll Daily Tasks</span>
              <ChevronRight className="w-4 h-4 text-dark-muted group-hover:translate-x-1 transition-transform" />
            </Link>

            <Link
              to="/devices"
              className="flex items-center justify-between p-4 rounded-xl border border-white/5 bg-white/[0.01] hover:bg-white/[0.03] text-sm font-semibold text-white group transition-colors"
            >
              <span>Manage Wearables</span>
              <ChevronRight className="w-4 h-4 text-dark-muted group-hover:translate-x-1 transition-transform" />
            </Link>
          </div>
        </div>
      </div>

      {showTelemetryModal && (
        <TelemetryModal
          userId={user.id}
          onSuccess={refreshUser}
          onClose={() => setShowTelemetryModal(false)}
        />
      )}
    </>
  );
};
