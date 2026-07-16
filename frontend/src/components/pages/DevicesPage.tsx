import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Spinner } from '../atoms/Spinner';
import { Button } from '../atoms/Button';
import { Badge } from '../atoms/Badge';
import { Cpu, Watch, Link2, Check, AlertCircle } from 'lucide-react';
import api from '../../services/api';

interface Device {
  id: number;
  name: string;
  brand: string;
  features: string[];
}

export const DevicesPage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [pairingId, setPairingId] = useState<number | null>(null);

  useEffect(() => {
    fetchDevices();
  }, []);

  const fetchDevices = async () => {
    try {
      setLoading(true);
      const response = await api.get('/device');
      setDevices(response.data || []);
    } catch (err) {
      console.error('Error fetching devices:', err);
    } finally {
      setLoading(false);
    }
  };

  const handlePair = async (deviceId: number) => {
    if (!user) return;
    try {
      setPairingId(deviceId);
      await api.put(`/user/${user.id}/device`, null, {
        params: { deviceId },
      });
      await refreshUser();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to pair device.');
    } finally {
      setPairingId(null);
    }
  };

  return (
    <>
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-white flex items-center gap-2">
            <Watch className="w-6 h-6 text-accent-cyan" />
            Smartwatch Integrations
          </h1>
          <p className="text-sm text-dark-muted mt-1">
            Pair your smartwatch device to feed sensory telemetry (heart rate, steps, GPS) to PULSE.IQ.
          </p>
        </div>
      </div>

      {/* Currently Paired Device */}
      <div className="glass-panel rounded-2xl p-6 border border-white/5">
        <h3 className="text-sm font-bold text-white mb-4">Linked Wearable Status</h3>
        
        {user?.device ? (
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-5 rounded-xl border border-accent-cyan/15 bg-accent-cyan/5">
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-lg bg-accent-cyan/15 text-accent-cyan">
                <Watch className="w-6 h-6" />
              </div>
              <div>
                <h4 className="text-base font-bold text-white">{user.device.name}</h4>
                <p className="text-xs text-dark-muted mt-0.5">Brand: {user.device.brand}</p>
                <div className="flex flex-wrap gap-1.5 mt-2">
                  {user.device.features.map((feat: string) => (
                    <span
                      key={feat}
                      className="px-2 py-0.5 rounded bg-accent-cyan/10 border border-accent-cyan/20 text-[9px] font-semibold text-accent-cyan"
                    >
                      {feat}
                    </span>
                  ))}
                </div>
              </div>
            </div>
            <span className="flex items-center gap-1.5 text-accent-emerald font-semibold text-sm px-3.5 py-2 bg-accent-emerald/10 border border-accent-emerald/20 rounded-lg">
              <Check className="w-4 h-4" /> Active pairing
            </span>
          </div>
        ) : (
          <div className="flex items-center gap-3 p-4 rounded-xl border border-accent-rose/15 bg-accent-rose/5 text-xs text-accent-rose">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <div>
              <p className="font-semibold">No smartwatch paired yet.</p>
              <p className="text-[11px] mt-0.5 text-accent-rose/80">
                Please choose a smartwatch model below to activate your account telemetry channels.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Available Devices Grid */}
      <h3 className="text-base font-bold text-white pt-4">Available Wearable Models</h3>
      
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <Spinner size="lg" />
          <p className="text-xs text-dark-muted">Searching compatible watch models...</p>
        </div>
      ) : devices.length === 0 ? (
        <div className="text-center py-12 glass-panel rounded-2xl border border-white/5">
          <Cpu className="w-10 h-10 text-dark-muted mx-auto mb-2" />
          <p className="text-sm text-dark-muted">No smartwatch device models found in the database.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {devices.map((device) => {
            const isCurrent = user?.device?.id === device.id;
            return (
              <div
                key={device.id}
                className={`glass-panel rounded-xl p-5 flex flex-col justify-between border transition-all duration-300 ${
                  isCurrent ? 'border-accent-cyan bg-accent-cyan/[0.02]' : 'border-white/5 hover:border-white/10'
                }`}
              >
                <div>
                  <h4 className="text-base font-bold text-white flex items-center justify-between">
                    <span>{device.name}</span>
                    <Badge variant="muted">{device.brand}</Badge>
                  </h4>
                  <p className="text-xs text-dark-muted mt-2">Sensors & hardware capabilities:</p>
                  <div className="flex flex-wrap gap-1.5 mt-2 mb-4">
                    {device.features.map((feat) => (
                      <span
                        key={feat}
                        className="px-1.5 py-0.5 rounded bg-white/5 border border-white/5 text-[9px] font-semibold text-dark-muted"
                      >
                        {feat}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="pt-3 border-t border-white/5 flex justify-end">
                  <Button
                    size="sm"
                    variant={isCurrent ? 'secondary' : 'primary'}
                    disabled={isCurrent || pairingId === device.id}
                    onClick={() => handlePair(device.id)}
                    className="gap-1.5"
                  >
                    {isCurrent ? (
                      <>
                        <Check className="w-3.5 h-3.5" /> Paired
                      </>
                    ) : pairingId === device.id ? (
                      'Linking...'
                    ) : (
                      <>
                        <Link2 className="w-3.5 h-3.5" /> Link Model
                      </>
                    )}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </>
  );
};
