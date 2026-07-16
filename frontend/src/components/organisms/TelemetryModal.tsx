import React, { useState } from 'react';
import { Button } from '../atoms/Button';
import { FormField } from '../molecules/FormField';
import { X, Send, Activity } from 'lucide-react';
import api from '../../services/api';

interface TelemetryModalProps {
  userId: number;
  onSuccess: () => void;
  onClose: () => void;
}

export const TelemetryModal: React.FC<TelemetryModalProps> = ({
  userId,
  onSuccess,
  onClose,
}) => {
  const [steps, setSteps] = useState('5000');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [selectedTags, setSelectedTags] = useState<string[]>(['HeartRate', 'GPS']);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const availableTags = ['HeartRate', 'GPS', 'Accelerometer', 'Gyroscope', 'Barometer', 'SpO2'];

  const handleTagToggle = (tag: string) => {
    setSelectedTags((prev) =>
      prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!steps || isNaN(Number(steps)) || Number(steps) <= 0) {
      setError('Please enter a valid step count.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      setMessage(null);

      const response = await api.post(`/user/${userId}`, {
        stepCountValue: Number(steps),
        date,
        tags: selectedTags,
      });

      setMessage(response.data?.message || 'Telemetry message published to Kafka!');
      setTimeout(() => {
        onSuccess();
        onClose();
      }, 1200);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit telemetry event.');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass-panel w-full max-w-md rounded-2xl overflow-hidden p-6 relative animate-in fade-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-white/5 mb-6">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-accent-cyan animate-pulse" />
            <h3 className="text-lg font-bold text-white">Ingest Watch Telemetry</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-dark-muted hover:text-white hover:bg-white/5 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <FormField
            label="Step Count"
            id="steps"
            type="number"
            value={steps}
            onChange={(e) => setSteps(e.target.value)}
            placeholder="e.g. 5000"
            required
          />

          <FormField
            label="Activity Date"
            id="date"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            required
          />

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-dark-muted mb-2">
              Sensor Tags (Telemetry Source)
            </label>
            <div className="grid grid-cols-2 gap-2">
              {availableTags.map((tag) => {
                const checked = selectedTags.includes(tag);
                return (
                  <button
                    type="button"
                    key={tag}
                    onClick={() => handleTagToggle(tag)}
                    className={`flex items-center justify-between px-3 py-2 rounded-lg text-xs border font-medium transition-all ${
                      checked
                        ? 'bg-accent-cyan/15 border-accent-cyan/40 text-accent-cyan shadow-sm'
                        : 'bg-white/5 border-transparent text-dark-muted hover:text-dark-text hover:bg-white/10'
                    }`}
                  >
                    <span>{tag}</span>
                    <span
                      className={`w-2.5 h-2.5 rounded-full ${
                        checked ? 'bg-accent-cyan' : 'bg-white/10'
                      }`}
                    />
                  </button>
                );
              })}
            </div>
          </div>

          {error && <p className="text-xs text-accent-rose font-medium mt-2">{error}</p>}
          {message && <p className="text-xs text-accent-emerald font-semibold mt-2">{message}</p>}

          <div className="pt-4 flex justify-end gap-3 border-t border-white/5 mt-6">
            <Button type="button" variant="secondary" onClick={onClose} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting} className="gap-2">
              <Send className="w-4 h-4" />
              {submitting ? 'Sending...' : 'Send Event'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
