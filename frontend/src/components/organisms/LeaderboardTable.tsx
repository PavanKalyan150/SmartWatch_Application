import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import { Spinner } from '../atoms/Spinner';
import { Badge } from '../atoms/Badge';
import { Trophy, Medal, Search, X } from 'lucide-react';

interface LeaderboardTableProps {
  challengeId: number;
  challengeTitle: string;
  onClose: () => void;
}

export const LeaderboardTable: React.FC<LeaderboardTableProps> = ({
  challengeId,
  challengeTitle,
  onClose,
}) => {
  const [participants, setParticipants] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchLeaderboard();
  }, [challengeId]);

  const fetchLeaderboard = async () => {
    try {
      setLoading(true);
      setError(null);
      // Fetch all participants. We'll use pageable sizing to retrieve first 50 participants
      const response = await api.get(`/challenge/${challengeId}/user`, {
        params: { size: 50, sort: 'score,desc' },
      });
      
      const list = response.data?.content || [];
      // Sort in-memory if ranks aren't calculated yet
      const sortedList = [...list].sort((a, b) => {
        if (a.rank && b.rank) return a.rank - b.rank;
        return b.score - a.score;
      });

      setParticipants(sortedList);
    } catch (err: any) {
      setError('Failed to load leaderboard participants.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const getRankIcon = (index: number, rank?: number) => {
    const finalRank = rank || (index + 1);
    if (finalRank === 1) return <Trophy className="w-5 h-5 text-yellow-400" />;
    if (finalRank === 2) return <Medal className="w-5 h-5 text-slate-300" />;
    if (finalRank === 3) return <Medal className="w-5 h-5 text-amber-600" />;
    return <span className="text-xs font-bold text-dark-muted w-5 text-center">{finalRank}</span>;
  };

  return (
    <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass-panel w-full max-w-3xl rounded-2xl overflow-hidden max-h-[85vh] flex flex-col relative animate-in fade-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-white">Challenge Leaderboard</h3>
            <p className="text-xs text-dark-muted mt-0.5">{challengeTitle}</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-dark-muted hover:text-white hover:bg-white/5 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content body */}
        <div className="p-6 overflow-y-auto flex-1 min-h-[300px]">
          {loading ? (
            <div className="flex flex-col items-center justify-center h-64 gap-3">
              <Spinner size="lg" />
              <p className="text-xs text-dark-muted">Retrieving rankings...</p>
            </div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center h-64 text-center">
              <p className="text-sm text-accent-rose font-medium">{error}</p>
              <button
                onClick={fetchLeaderboard}
                className="mt-4 text-xs text-primary hover:underline font-semibold"
              >
                Try Again
              </button>
            </div>
          ) : participants.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-center">
              <Search className="w-8 h-8 text-dark-muted mb-3" />
              <p className="text-sm text-dark-muted">No participants have joined this challenge yet.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/5 text-xs font-bold uppercase tracking-wider text-dark-muted">
                    <th className="pb-3 pl-4">Rank</th>
                    <th className="pb-3">User</th>
                    <th className="pb-3 text-right">Score (Steps)</th>
                    <th className="pb-3 text-center">Status</th>
                    <th className="pb-3 text-right pr-4">Points Earned</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-sm">
                  {participants.map((participant, index) => {
                    const finalRank = participant.rank || (index + 1);
                    return (
                      <tr
                        key={participant.id}
                        className={`transition-colors hover:bg-white/2.5 ${
                          finalRank <= 3 ? 'bg-white/[0.01]' : ''
                        }`}
                      >
                        <td className="py-3.5 pl-4 flex items-center gap-2">
                          {getRankIcon(index, participant.rank)}
                        </td>
                        <td className="py-3.5 font-medium text-white">
                          {participant.user?.fullName || 'Anonymous Participant'}
                        </td>
                        <td className="py-3.5 text-right font-semibold text-accent-cyan">
                          {participant.score.toLocaleString()}
                        </td>
                        <td className="py-3.5 text-center">
                          <Badge variant={participant.completed ? 'emerald' : 'muted'}>
                            {participant.completed ? 'Completed' : 'Active'}
                          </Badge>
                        </td>
                        <td className="py-3.5 text-right pr-4 font-bold text-accent-purple">
                          {participant.pointsAwarded ? `+${participant.pointsAwarded} XP` : '--'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
