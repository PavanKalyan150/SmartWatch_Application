import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { ChallengeCard } from '../organisms/ChallengeCard';
import type { Challenge } from '../organisms/ChallengeCard';
import { LeaderboardTable } from '../organisms/LeaderboardTable';
import { Spinner } from '../atoms/Spinner';
import { FormField } from '../molecules/FormField';
import { Button } from '../atoms/Button';
import { Trophy, Search, MapPin, Navigation } from 'lucide-react';
import api from '../../services/api';

export const ChallengesPage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [joinedChallengeIds, setJoinedChallengeIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [joiningId, setJoiningId] = useState<number | null>(null);
  const [activeLeaderboard, setActiveLeaderboard] = useState<Challenge | null>(null);

  // Discovery Filter inputs
  const [city, setCity] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [filtering, setFiltering] = useState(false);

  useEffect(() => {
    if (user) {
      fetchChallengesAndRegistrations();
    }
  }, [user?.id]);

  const fetchChallengesAndRegistrations = async (useFilters = false) => {
    if (!user) return;
    try {
      setLoading(true);
      
      // 1. Fetch joined challenges
      const regResponse = await api.get(`/user/${user.id}/challenges`);
      const joinedIds = (regResponse.data || []).map((reg: any) => reg.challenge?.id);
      setJoinedChallengeIds(joinedIds);

      // 2. Fetch/Discover challenges with query params
      const params: any = { userId: user.id };
      if (useFilters) {
        if (city.trim()) params.city = city.trim();
        if (latitude.trim()) params.latitude = Number(latitude);
        if (longitude.trim()) params.longitude = Number(longitude);
      }

      const challengeResponse = await api.get('/challenge', { params });
      setChallenges(challengeResponse.data || []);
    } catch (err) {
      console.error('Error fetching challenges:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFiltering(true);
    fetchChallengesAndRegistrations(true);
  };

  const handleClearFilters = () => {
    setCity('');
    setLatitude('');
    setLongitude('');
    setFiltering(false);
    fetchChallengesAndRegistrations(false);
  };

  const handleJoin = async (challengeId: number) => {
    if (!user) return;
    try {
      setJoiningId(challengeId);
      await api.post(`/challenge/${challengeId}/register`, null, {
        params: { userId: user.id },
      });
      // Refresh user stats (points could update, or registrations)
      await fetchChallengesAndRegistrations(filtering);
      await refreshUser();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to join challenge. Make sure your device has the required sensors.');
    } finally {
      setJoiningId(null);
    }
  };

  const handleUseMockLocation = () => {
    // San Francisco mock location
    setCity('San Francisco');
    setLatitude('37.7749');
    setLongitude('-122.4194');
  };

  return (
    <>
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-white flex items-center gap-2">
            <Trophy className="w-6 h-6 text-primary" />
            Discover Fitness Challenges
          </h1>
          <p className="text-sm text-dark-muted mt-1">
            Join community challenges, map your GPS workouts, and rise in the leaderboard.
          </p>
        </div>
      </div>

      {/* Geospatial and City Filters */}
      <div className="glass-panel rounded-2xl p-6 border border-white/5">
        <h3 className="text-sm font-bold text-white mb-4 flex items-center gap-2">
          <MapPin className="w-4 h-4 text-accent-cyan" />
          Location Discovery Engine
        </h3>
        
        <form onSubmit={handleFilterSubmit} className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-end">
          <FormField
            label="City Name"
            id="city"
            type="text"
            placeholder="e.g. San Francisco"
            value={city}
            onChange={(e) => setCity(e.target.value)}
          />
          <FormField
            label="Latitude"
            id="latitude"
            type="number"
            step="any"
            placeholder="e.g. 37.7749"
            value={latitude}
            onChange={(e) => setLatitude(e.target.value)}
          />
          <FormField
            label="Longitude"
            id="longitude"
            type="number"
            step="any"
            placeholder="e.g. -122.4194"
            value={longitude}
            onChange={(e) => setLongitude(e.target.value)}
          />

          <div className="sm:col-span-3 flex flex-wrap gap-3 mt-2 justify-between">
            <Button
              type="button"
              variant="glass"
              size="sm"
              onClick={handleUseMockLocation}
              className="gap-1.5 text-xs text-accent-cyan"
            >
              <Navigation className="w-3.5 h-3.5" />
              Use SF Mock Coordinates
            </Button>
            <div className="flex gap-2">
              {filtering && (
                <Button type="button" variant="secondary" size="sm" onClick={handleClearFilters}>
                  Clear Filters
                </Button>
              )}
              <Button type="submit" size="sm" className="gap-1.5">
                <Search className="w-4 h-4" />
                Scan Area
              </Button>
            </div>
          </div>
        </form>
      </div>

      {/* Challenges Grid */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-24 gap-3">
          <Spinner size="lg" />
          <p className="text-xs text-dark-muted">Searching challenges...</p>
        </div>
      ) : challenges.length === 0 ? (
        <div className="text-center py-20 glass-panel rounded-2xl border border-white/5">
          <Trophy className="w-12 h-12 text-dark-muted mx-auto mb-3" />
          <p className="text-sm font-semibold text-white">No matching challenges found</p>
          <p className="text-xs text-dark-muted mt-1 max-w-sm mx-auto">
            Try adjusting your search criteria, checking global challenges, or clearing the geo-filters.
          </p>
          {filtering && (
            <Button variant="glass" size="sm" onClick={handleClearFilters} className="mt-4">
              Reset Filters
            </Button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {challenges.map((challenge) => (
            <ChallengeCard
              key={challenge.id}
              challenge={challenge}
              isJoined={joinedChallengeIds.includes(challenge.id)}
              isJoining={joiningId === challenge.id}
              userDevice={user?.device}
              onJoin={handleJoin}
              onViewLeaderboard={(c) => setActiveLeaderboard(c)}
            />
          ))}
        </div>
      )}

      {/* Leaderboard Modal overlay */}
      {activeLeaderboard && (
        <LeaderboardTable
          challengeId={activeLeaderboard.id}
          challengeTitle={activeLeaderboard.title}
          onClose={() => setActiveLeaderboard(null)}
        />
      )}
    </>
  );
};
