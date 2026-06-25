import { useState, type FormEvent } from 'react';
import { getWifiConfiguration } from '../api/wifiApi';
import { WifiConfiguration } from '../types/wifi';
import { ErrorAlert } from './ErrorAlert';

interface Props {
  onFound: (config: WifiConfiguration) => void;
}

export function WifiLookupForm({ onFound }: Props) {
  const [cpeId, setCpeId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = cpeId.trim();
    if (!trimmed) {
      setError('CPE ID is required');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const config = await getWifiConfiguration(trimmed);
      onFound(config);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lookup failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="card">
      <h2>Look Up CPE Device</h2>
      <form onSubmit={handleSubmit} className="lookup-form">
        <div className="form-row">
          <label htmlFor="lookup-cpe-id">CPE ID</label>
          <input
            id="lookup-cpe-id"
            type="text"
            value={cpeId}
            onChange={e => setCpeId(e.target.value)}
            placeholder="e.g. CPE_001"
            maxLength={64}
            disabled={loading}
            autoFocus
          />
          <button type="submit" disabled={loading || !cpeId.trim()}>
            {loading ? 'Searching…' : 'Search'}
          </button>
        </div>
      </form>
      {error && <ErrorAlert message={error} onDismiss={() => setError(null)} />}
    </section>
  );
}
