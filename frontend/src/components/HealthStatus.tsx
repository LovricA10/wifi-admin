import { useEffect, useState } from 'react';
import { getHealthStatus } from '../api/wifiApi';

type Status = 'UP' | 'DOWN' | 'LOADING';

const STATUS_LABELS: Record<Status, string> = {
  UP: 'Backend UP',
  DOWN: 'Backend DOWN',
  LOADING: 'Checking...',
};

const STATUS_COLORS: Record<Status, string> = {
  UP: '#22c55e',
  DOWN: '#ef4444',
  LOADING: '#94a3b8',
};

export function HealthStatus() {
  const [status, setStatus] = useState<Status>('LOADING');

  useEffect(() => {
    const check = async () => {
      const health = await getHealthStatus();
      setStatus(health.status === 'UP' ? 'UP' : 'DOWN');
    };

    check();
    const interval = setInterval(check, 30_000);
    return () => clearInterval(interval);
  }, []);

  const color = STATUS_COLORS[status];

  return (
    <span className="health-badge" style={{ color }}>
      <span className="health-dot" style={{ background: color }} />
      {STATUS_LABELS[status]}
    </span>
  );
}
