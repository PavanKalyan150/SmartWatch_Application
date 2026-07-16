import React from 'react';
import { Badge } from '../atoms/Badge';
import { Button } from '../atoms/Button';
import { Calendar, MapPin, Cpu, CheckCircle2, AlertTriangle, Trophy } from 'lucide-react';

export interface Challenge {
  id: number;
  title: string;
  description: string;
  requiredSteps: number;
  pointsReward: number;
  expiryDate: string;
  requiredFeatures: string[];
  latitude?: number;
  longitude?: number;
  radiusKm?: number;
  city?: string;
  isGlobal: boolean;
  isProcessed: boolean;
}

interface ChallengeCardProps {
  challenge: Challenge;
  isJoined: boolean;
  isJoining: boolean;
  userDevice: any;
  onJoin: (challengeId: number) => void;
  onViewLeaderboard?: (challenge: Challenge) => void;
}

export const ChallengeCard: React.FC<ChallengeCardProps> = ({
  challenge,
  isJoined,
  isJoining,
  userDevice,
  onJoin,
  onViewLeaderboard,
}) => {
  const isExpired = new Date(challenge.expiryDate) < new Date();

  // Check device compatibility
  const hasFeatures = challenge.requiredFeatures && challenge.requiredFeatures.length > 0;
  const isCompatible = !hasFeatures || (
    userDevice &&
    challenge.requiredFeatures.every((f: string) => userDevice.features?.includes(f))
  );

  return (
    <div className="glass-panel rounded-xl p-6 flex flex-col justify-between transition-all duration-300 border border-white/5 hover:border-white/10 relative overflow-hidden">
      {/* Accent gradient line */}
      <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${isJoined ? 'from-accent-emerald to-teal-400' : 'from-primary to-accent-cyan'}`} />

      <div>
        <div className="flex items-start justify-between gap-4 mb-3">
          <h4 className="text-lg font-bold text-white tracking-tight leading-snug">
            {challenge.title}
          </h4>
          <div className="flex flex-col items-end gap-1.5 shrink-0">
            <span className="text-xs font-bold text-primary flex items-center gap-1">
              <Trophy className="w-3.5 h-3.5" />
              +{challenge.pointsReward} XP
            </span>
            <Badge variant={challenge.isGlobal ? 'cyan' : 'purple'}>
              {challenge.isGlobal ? 'Global' : 'Local'}
            </Badge>
          </div>
        </div>

        <p className="text-sm text-dark-muted mb-4 line-clamp-2 leading-relaxed">
          {challenge.description}
        </p>

        {/* Challenge Scope/Location Details */}
        <div className="space-y-2 mb-4 text-xs">
          <div className="flex items-center gap-2 text-dark-muted">
            <Calendar className="w-3.5 h-3.5 text-primary" />
            <span>Expires: {new Date(challenge.expiryDate).toLocaleDateString()}</span>
          </div>

          <div className="flex items-center gap-2 text-dark-muted">
            <MapPin className="w-3.5 h-3.5 text-accent-cyan" />
            {challenge.isGlobal ? (
              <span>Available Worldwide</span>
            ) : challenge.city ? (
              <span>City: {challenge.city}</span>
            ) : (
              <span>Radius: {challenge.radiusKm} km from coordinates</span>
            )}
          </div>

          <div className="flex items-center gap-2 text-dark-muted">
            <CheckCircle2 className="w-3.5 h-3.5 text-accent-emerald" />
            <span>Goal: {challenge.requiredSteps.toLocaleString()} steps</span>
          </div>
        </div>

        {/* Required Features List */}
        {hasFeatures && (
          <div className="border-t border-white/5 pt-3 mt-3 mb-4">
            <p className="text-[10px] font-semibold uppercase tracking-wider text-dark-muted mb-2 flex items-center gap-1.5">
              <Cpu className="w-3 h-3" />
              Required Watch Sensors:
            </p>
            <div className="flex flex-wrap gap-1.5">
              {challenge.requiredFeatures.map((feat) => (
                <span
                  key={feat}
                  className={`px-2 py-0.5 rounded text-[9px] font-medium border ${
                    userDevice?.features?.includes(feat)
                      ? 'bg-accent-emerald/5 border-accent-emerald/20 text-accent-emerald'
                      : 'bg-accent-rose/5 border-accent-rose/20 text-accent-rose'
                  }`}
                >
                  {feat}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Action Footer */}
      <div className="mt-4 pt-4 border-t border-white/5 flex gap-2 items-center justify-between">
        {!isCompatible && !isExpired && (
          <div className="flex items-center gap-1 text-[10px] text-accent-rose font-medium">
            <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
            <span>Incompatible Device</span>
          </div>
        )}
        {isExpired && (
          <Badge variant="rose">Expired</Badge>
        )}
        {!isExpired && isCompatible && !isJoined && <div />}

        <div className="flex gap-2 w-full justify-end">
          {challenge.isProcessed && onViewLeaderboard && (
            <Button size="sm" variant="glass" onClick={() => onViewLeaderboard(challenge)}>
              Leaderboard
            </Button>
          )}

          {!isExpired && (
            isJoined ? (
              <span className="flex items-center gap-1 text-accent-emerald font-semibold text-xs px-3 py-1.5 bg-accent-emerald/10 border border-accent-emerald/20 rounded-lg">
                <CheckCircle2 className="w-4 h-4" /> Joined
              </span>
            ) : (
              <Button
                size="sm"
                variant={isCompatible ? 'primary' : 'secondary'}
                disabled={isJoining || !isCompatible}
                onClick={() => onJoin(challenge.id)}
              >
                {isJoining ? 'Joining...' : 'Join Challenge'}
              </Button>
            )
          )}
        </div>
      </div>
    </div>
  );
};
