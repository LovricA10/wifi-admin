import { useState } from 'react';
import { WifiConfiguration } from './types/wifi';
import { WifiLookupForm } from './components/WifiLookupForm';
import { WifiConfigurationForm } from './components/WifiConfigurationForm';
import { HealthStatus } from './components/HealthStatus';
import { WifiIcon } from './svg/WifiIcon';

export function App() {
  const [config, setConfig] = useState<WifiConfiguration | null>(null);

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header-inner">
          <h1><WifiIcon size={20} color="var(--color-primary)" /> WiFi Admin</h1>
          <HealthStatus />
        </div>
      </header>

      <main className="app-main">
        <WifiLookupForm onFound={setConfig} />
        {config && (
          <WifiConfigurationForm
            key={config.cpeId}
            config={config}
            onUpdated={setConfig}
          />
        )}
      </main>

      <footer className="app-footer">
        WiFi Admin &mdash; REST to SOAP integration
      </footer>
    </div>
  );
}
