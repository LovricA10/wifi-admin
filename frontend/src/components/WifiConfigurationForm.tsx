import { useState, type FormEvent } from 'react';
import { updateWifiConfiguration } from '../api/wifiApi';
import { WifiConfiguration, WifiBand, EncryptionType } from '../types/wifi';
import { ErrorAlert } from './ErrorAlert';

interface Props {
  config: WifiConfiguration;
  onUpdated: (config: WifiConfiguration) => void;
}

interface FormState {
  cpeId: string;
  wifiBand: WifiBand;
  ssid: string;
  encryptionType: EncryptionType;
  password: string;
}

interface FieldErrors {
  ssid?: string;
  password?: string;
}

const WIFI_BANDS: WifiBand[] = ['BAND_2_4_GHZ', 'BAND_5_GHZ'];

const BAND_LABELS: Record<WifiBand, string> = {
  BAND_2_4_GHZ: '2.4 GHz',
  BAND_5_GHZ: '5 GHz',
};

const ENCRYPTION_TYPES: EncryptionType[] = [
  'OPEN',
  'WEP',
  'WPA_PSK',
  'WPA2_PSK',
  'WPA3_SAE',
  'WPA2_ENTERPRISE',
];

function requiresPassword(encryptionType: EncryptionType): boolean {
  return encryptionType !== 'OPEN';
}

function toFormState(config: WifiConfiguration): FormState {
  return {
    cpeId: config.cpeId,
    wifiBand: config.wifiBand,
    ssid: config.ssid,
    encryptionType: config.encryptionType ?? 'OPEN',
    password: config.password ?? '',
  };
}

export function WifiConfigurationForm({ config, onUpdated }: Props) {
  const [form, setForm] = useState<FormState>(toFormState(config));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const passwordRequired = requiresPassword(form.encryptionType);

  const validate = (): boolean => {
    const errors: FieldErrors = {};
    if (!form.ssid.trim()) {
      errors.ssid = 'SSID is required';
    } else if (form.ssid.trim().length > 32) {
      errors.ssid = 'SSID must be at most 32 characters';
    }
    if (passwordRequired && !form.password.trim()) {
      errors.password = `Password is required for ${form.encryptionType}`;
    }
    if (form.password.length > 128) {
      errors.password = 'Password must be at most 128 characters';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setError(null);
    setSuccess(false);

    const payload: WifiConfiguration = {
      cpeId: form.cpeId,
      wifiBand: form.wifiBand,
      ssid: form.ssid.trim(),
      encryptionType: form.encryptionType,
      password: passwordRequired ? form.password.trim() || null : null,
    };

    try {
      const updated = await updateWifiConfiguration(payload);
      onUpdated(updated);
      setForm(toFormState(updated));
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setLoading(false);
    }
  };

  const handleEncryptionChange = (newType: EncryptionType) => {
    setForm(prev => ({
      ...prev,
      encryptionType: newType,
      password: requiresPassword(newType) ? prev.password : '',
    }));
    setFieldErrors(prev => ({ ...prev, password: undefined }));
    setSuccess(false);
  };

  const clearFieldError = (field: keyof FieldErrors) => {
    setFieldErrors(prev => ({ ...prev, [field]: undefined }));
    setSuccess(false);
  };

  return (
    <section className="card">
      <h2>
        WiFi Configuration &mdash;{' '}
        <span className="cpe-id-label">{config.cpeId}</span>
      </h2>
      <form onSubmit={handleSubmit} noValidate>
        <div className="form-group">
          <label htmlFor="cfg-cpe-id">CPE ID</label>
          <input
            id="cfg-cpe-id"
            type="text"
            value={form.cpeId}
            disabled
            className="input-readonly"
          />
        </div>

        <div className="form-group">
          <label htmlFor="cfg-wifi-band">WiFi Band</label>
          <select
            id="cfg-wifi-band"
            value={form.wifiBand}
            onChange={e => {
              setForm(prev => ({ ...prev, wifiBand: e.target.value as WifiBand }));
              setSuccess(false);
            }}
            disabled={loading}
          >
            {WIFI_BANDS.map(band => (
              <option key={band} value={band}>
                {BAND_LABELS[band]}
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="cfg-ssid">SSID</label>
          <input
            id="cfg-ssid"
            type="text"
            value={form.ssid}
            onChange={e => {
              setForm(prev => ({ ...prev, ssid: e.target.value }));
              clearFieldError('ssid');
            }}
            maxLength={32}
            disabled={loading}
            className={fieldErrors.ssid ? 'input-error' : ''}
          />
          {fieldErrors.ssid && (
            <span className="field-error">{fieldErrors.ssid}</span>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="cfg-encryption">Encryption Type</label>
          <select
            id="cfg-encryption"
            value={form.encryptionType}
            onChange={e => handleEncryptionChange(e.target.value as EncryptionType)}
            disabled={loading}
          >
            {ENCRYPTION_TYPES.map(type => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </div>

        {passwordRequired && (
          <div className="form-group">
            <label htmlFor="cfg-password">Password</label>
            <input
              id="cfg-password"
              type="password"
              value={form.password}
              onChange={e => {
                setForm(prev => ({ ...prev, password: e.target.value }));
                clearFieldError('password');
              }}
              maxLength={128}
              disabled={loading}
              autoComplete="new-password"
              className={fieldErrors.password ? 'input-error' : ''}
            />
            {fieldErrors.password && (
              <span className="field-error">{fieldErrors.password}</span>
            )}
          </div>
        )}

        {error && (
          <ErrorAlert message={error} onDismiss={() => setError(null)} />
        )}
        {success && (
          <div className="success-alert" role="status">
            Configuration updated successfully.
          </div>
        )}

        <div className="form-actions">
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Updating…' : 'Update Configuration'}
          </button>
        </div>
      </form>
    </section>
  );
}
