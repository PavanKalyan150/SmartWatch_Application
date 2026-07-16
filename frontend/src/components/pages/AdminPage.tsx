import React, { useState } from 'react';
import { Button } from '../atoms/Button';
import { FormField } from '../molecules/FormField';
import { ShieldCheck, Play, Plus, Smartphone, Trophy, ListTodo, CheckCircle, AlertTriangle } from 'lucide-react';
import api from '../../services/api';

export const AdminPage: React.FC = () => {
  // Batch status
  const [batchLoading, setBatchLoading] = useState<string | null>(null);
  const [batchMessage, setBatchMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Form states
  const [activeTab, setActiveTab] = useState<'challenge' | 'task' | 'device'>('challenge');

  // Device form
  const [deviceName, setDeviceName] = useState('');
  const [deviceBrand, setDeviceBrand] = useState('');
  const [deviceFeatures, setDeviceFeatures] = useState('HeartRate, GPS, Accelerometer');

  // Task form
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDesc, setTaskDesc] = useState('');
  const [taskSteps, setTaskSteps] = useState('10000');
  const [taskPoints, setTaskPoints] = useState('100');

  // Challenge form
  const [challengeTitle, setChallengeTitle] = useState('');
  const [challengeDesc, setChallengeDesc] = useState('');
  const [challengeSteps, setChallengeSteps] = useState('50000');
  const [challengePoints, setChallengePoints] = useState('500');
  const [challengeExpiry, setChallengeExpiry] = useState('');
  const [challengeFeatures, setChallengeFeatures] = useState('HeartRate, GPS');
  const [challengeCity, setChallengeCity] = useState('');
  const [challengeLat, setChallengeLat] = useState('');
  const [challengeLon, setChallengeLon] = useState('');
  const [challengeRadius, setChallengeRadius] = useState('');

  const [formSuccess, setFormSuccess] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const runBatchJob = async (endpoint: 'rank' | 'game') => {
    try {
      setBatchLoading(endpoint);
      setBatchMessage(null);
      const response = await api.get(`/${endpoint}`);
      setBatchMessage({
        type: 'success',
        text: response.data?.message || `Job /${endpoint} triggered successfully!`,
      });
    } catch (err: any) {
      setBatchMessage({
        type: 'error',
        text: err.response?.data?.message || `Failed to trigger /${endpoint} job.`,
      });
    } finally {
      setBatchLoading(null);
    }
  };

  const handleCreateDevice = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setFormError(null);
      setFormSuccess(null);
      
      const featuresArr = deviceFeatures.split(',').map((f) => f.trim()).filter((f) => f.length > 0);
      await api.post('/device', {
        name: deviceName,
        brand: deviceBrand,
        features: featuresArr,
      });

      setFormSuccess(`Device Model "${deviceName}" created successfully!`);
      setDeviceName('');
      setDeviceBrand('');
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to create device.');
    }
  };

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setFormError(null);
      setFormSuccess(null);

      await api.post('/task', {
        title: taskTitle,
        description: taskDesc,
        targetSteps: Number(taskSteps),
        pointsReward: Number(taskPoints),
      });

      setFormSuccess(`Daily Task "${taskTitle}" created successfully!`);
      setTaskTitle('');
      setTaskDesc('');
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to create task.');
    }
  };

  const handleCreateChallenge = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setFormError(null);
      setFormSuccess(null);

      if (!challengeExpiry) {
        setFormError('Please select a valid expiry date and time.');
        return;
      }

      const featuresArr = challengeFeatures.split(',').map((f) => f.trim()).filter((f) => f.length > 0);
      const body: any = {
        title: challengeTitle,
        description: challengeDesc,
        requiredSteps: Number(challengeSteps),
        pointsReward: Number(challengePoints),
        expiryDate: new Date(challengeExpiry).toISOString(),
        requiredFeatures: featuresArr,
      };

      if (challengeCity.trim()) body.city = challengeCity.trim();
      if (challengeLat.trim()) body.latitude = Number(challengeLat);
      if (challengeLon.trim()) body.longitude = Number(challengeLon);
      if (challengeRadius.trim()) body.radiusKm = Number(challengeRadius);

      await api.post('/challenge', body);

      setFormSuccess(`Challenge "${challengeTitle}" created successfully!`);
      setChallengeTitle('');
      setChallengeDesc('');
      setChallengeCity('');
      setChallengeLat('');
      setChallengeLon('');
      setChallengeRadius('');
    } catch (err: any) {
      setFormError(err.response?.data?.message || 'Failed to create challenge.');
    }
  };

  return (
    <>
      <div>
        <h1 className="text-2xl font-extrabold tracking-tight text-white flex items-center gap-2">
          <ShieldCheck className="w-6 h-6 text-accent-rose animate-pulse" />
          Administrative Control Center
        </h1>
        <p className="text-sm text-dark-muted mt-1">
          Perform batch updates, generate entities, and audit smartwatch leaderboards.
        </p>
      </div>

      {/* Batch Processing Panel */}
      <div className="glass-panel rounded-2xl p-6 border border-white/5 space-y-6">
        <h3 className="text-base font-bold text-white">Manual Spring Batch Triggers</h3>
        <p className="text-xs text-dark-muted leading-relaxed">
          The database utilizes Spring Batch jobs to reconcile rankings and award points. Trigger them manually below:
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl border border-white/5 bg-white/[0.01] flex items-center justify-between gap-4">
            <div>
              <h4 className="text-sm font-bold text-white">Recalculate User Rankings</h4>
              <p className="text-xs text-dark-muted mt-1 leading-snug">
                Resolves expired challenges, calculates leaderboard tiers, and awards first/second place XP bonuses.
              </p>
            </div>
            <Button
              size="sm"
              disabled={batchLoading !== null}
              onClick={() => runBatchJob('rank')}
              className="gap-1.5 shrink-0"
            >
              <Play className="w-3.5 h-3.5" />
              {batchLoading === 'rank' ? 'Running...' : 'Run Job'}
            </Button>
          </div>

          <div className="p-4 rounded-xl border border-white/5 bg-white/[0.01] flex items-center justify-between gap-4">
            <div>
              <h4 className="text-sm font-bold text-white">Award Milestones & Badges</h4>
              <p className="text-xs text-dark-muted mt-1 leading-snug">
                Audits user cumulative points and updates tiers (Novice, Athlete, Champion), registering corresponding badges.
              </p>
            </div>
            <Button
              size="sm"
              disabled={batchLoading !== null}
              onClick={() => runBatchJob('game')}
              className="gap-1.5 shrink-0"
            >
              <Play className="w-3.5 h-3.5" />
              {batchLoading === 'game' ? 'Run Job' : 'Run Job'}
            </Button>
          </div>
        </div>

        {batchMessage && (
          <div
            className={`p-4 rounded-xl border flex items-start gap-2.5 text-xs ${
              batchMessage.type === 'success'
                ? 'border-accent-emerald/15 bg-accent-emerald/5 text-accent-emerald'
                : 'border-accent-rose/15 bg-accent-rose/5 text-accent-rose'
            }`}
          >
            {batchMessage.type === 'success' ? (
              <CheckCircle className="w-4 h-4 mt-0.5 shrink-0" />
            ) : (
              <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
            )}
            <span>{batchMessage.text}</span>
          </div>
        )}
      </div>

      {/* Admin Creator Workspace */}
      <div className="glass-panel rounded-2xl overflow-hidden border border-white/5">
        <div className="flex border-b border-white/5 bg-white/[0.02]">
          <button
            onClick={() => { setActiveTab('challenge'); setFormSuccess(null); setFormError(null); }}
            className={`flex-1 py-4 text-sm font-bold border-b-2 transition-colors flex justify-center items-center gap-2 ${
              activeTab === 'challenge'
                ? 'border-primary text-primary bg-primary/5'
                : 'border-transparent text-dark-muted hover:text-white hover:bg-white/5'
            }`}
          >
            <Trophy className="w-4 h-4" /> Challenge Creator
          </button>
          <button
            onClick={() => { setActiveTab('task'); setFormSuccess(null); setFormError(null); }}
            className={`flex-1 py-4 text-sm font-bold border-b-2 transition-colors flex justify-center items-center gap-2 ${
              activeTab === 'task'
                ? 'border-primary text-primary bg-primary/5'
                : 'border-transparent text-dark-muted hover:text-white hover:bg-white/5'
            }`}
          >
            <ListTodo className="w-4 h-4" /> Task Creator
          </button>
          <button
            onClick={() => { setActiveTab('device'); setFormSuccess(null); setFormError(null); }}
            className={`flex-1 py-4 text-sm font-bold border-b-2 transition-colors flex justify-center items-center gap-2 ${
              activeTab === 'device'
                ? 'border-primary text-primary bg-primary/5'
                : 'border-transparent text-dark-muted hover:text-white hover:bg-white/5'
            }`}
          >
            <Smartphone className="w-4 h-4" /> Device Creator
          </button>
        </div>

        <div className="p-6">
          {formSuccess && (
            <p className="mb-4 text-xs font-semibold text-accent-emerald p-3 bg-accent-emerald/10 border border-accent-emerald/20 rounded-lg">
              {formSuccess}
            </p>
          )}
          {formError && (
            <p className="mb-4 text-xs font-semibold text-accent-rose p-3 bg-accent-rose/10 border border-accent-rose/20 rounded-lg">
              {formError}
            </p>
          )}

          {/* CHALLENGE FORM */}
          {activeTab === 'challenge' && (
            <form onSubmit={handleCreateChallenge} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField
                  label="Challenge Title"
                  id="title"
                  type="text"
                  placeholder="e.g. SF Summer Marathon"
                  value={challengeTitle}
                  onChange={(e) => setChallengeTitle(e.target.value)}
                  required
                />
                <FormField
                  label="Sensor Requirements (Comma Separated)"
                  id="features"
                  type="text"
                  placeholder="e.g. HeartRate, GPS"
                  value={challengeFeatures}
                  onChange={(e) => setChallengeFeatures(e.target.value)}
                  required
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <FormField
                  label="Required Step Count"
                  id="reqsteps"
                  type="number"
                  placeholder="e.g. 50000"
                  value={challengeSteps}
                  onChange={(e) => setChallengeSteps(e.target.value)}
                  required
                />
                <FormField
                  label="XP Reward Value"
                  id="reward"
                  type="number"
                  placeholder="e.g. 500"
                  value={challengePoints}
                  onChange={(e) => setChallengePoints(e.target.value)}
                  required
                />
                <FormField
                  label="Challenge Expiration"
                  id="expiry"
                  type="datetime-local"
                  value={challengeExpiry}
                  onChange={(e) => setChallengeExpiry(e.target.value)}
                  required
                />
              </div>

              <div className="border-t border-white/5 pt-4">
                <h4 className="text-xs font-bold text-white mb-3">Geospatial Boundaries (Optional - leave empty for Global challenges)</h4>
                
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
                  <FormField
                    label="Target City"
                    id="ccity"
                    type="text"
                    placeholder="e.g. San Francisco"
                    value={challengeCity}
                    onChange={(e) => setChallengeCity(e.target.value)}
                  />
                  <FormField
                    label="Center Latitude"
                    id="clat"
                    type="number"
                    step="any"
                    placeholder="e.g. 37.7749"
                    value={challengeLat}
                    onChange={(e) => setChallengeLat(e.target.value)}
                  />
                  <FormField
                    label="Center Longitude"
                    id="clon"
                    type="number"
                    step="any"
                    placeholder="e.g. -122.4194"
                    value={challengeLon}
                    onChange={(e) => setChallengeLon(e.target.value)}
                  />
                  <FormField
                    label="Coverage Radius (KM)"
                    id="crad"
                    type="number"
                    placeholder="e.g. 15"
                    value={challengeRadius}
                    onChange={(e) => setChallengeRadius(e.target.value)}
                  />
                </div>
              </div>

              <div className="pt-4 border-t border-white/5 flex justify-end">
                <Button type="submit" className="gap-2">
                  <Plus className="w-4 h-4" /> Create Challenge
                </Button>
              </div>
            </form>
          )}

          {/* TASK FORM */}
          {activeTab === 'task' && (
            <form onSubmit={handleCreateTask} className="space-y-4">
              <FormField
                label="Task Title"
                id="taskTitle"
                type="text"
                placeholder="e.g. Daily Step Target"
                value={taskTitle}
                onChange={(e) => setTaskTitle(e.target.value)}
                required
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField
                  label="Target Steps Count"
                  id="taskSteps"
                  type="number"
                  placeholder="e.g. 10000"
                  value={taskSteps}
                  onChange={(e) => setTaskSteps(e.target.value)}
                  required
                />
                <FormField
                  label="XP Reward Value"
                  id="taskPoints"
                  type="number"
                  placeholder="e.g. 100"
                  value={taskPoints}
                  onChange={(e) => setTaskPoints(e.target.value)}
                  required
                />
              </div>

              <div className="mb-4">
                <label className="block text-xs font-semibold uppercase tracking-wider text-dark-muted mb-1.5">
                  Task Description
                </label>
                <textarea
                  value={taskDesc}
                  onChange={(e) => setTaskDesc(e.target.value)}
                  placeholder="Summarize task goal..."
                  rows={3}
                  className="w-full px-4 py-2.5 rounded-lg glass-input focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 text-sm transition-all"
                  required
                />
              </div>

              <div className="pt-4 border-t border-white/5 flex justify-end">
                <Button type="submit" className="gap-2">
                  <Plus className="w-4 h-4" /> Create Task
                </Button>
              </div>
            </form>
          )}

          {/* DEVICE FORM */}
          {activeTab === 'device' && (
            <form onSubmit={handleCreateDevice} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField
                  label="Device Model Name"
                  id="dname"
                  type="text"
                  placeholder="e.g. Watch Series X"
                  value={deviceName}
                  onChange={(e) => setDeviceName(e.target.value)}
                  required
                />
                <FormField
                  label="Brand Name"
                  id="dbrand"
                  type="text"
                  placeholder="e.g. Apple, Samsung, Fitbit"
                  value={deviceBrand}
                  onChange={(e) => setDeviceBrand(e.target.value)}
                  required
                />
              </div>

              <FormField
                label="Hardware Sensor List (Comma Separated)"
                id="dfeatures"
                type="text"
                placeholder="e.g. HeartRate, GPS, Accelerometer, Gyroscope"
                value={deviceFeatures}
                onChange={(e) => setDeviceFeatures(e.target.value)}
                required
              />

              <div className="pt-4 border-t border-white/5 flex justify-end">
                <Button type="submit" className="gap-2">
                  <Plus className="w-4 h-4" /> Register Smartwatch Model
                </Button>
              </div>
            </form>
          )}
        </div>
      </div>
    </>
  );
};
