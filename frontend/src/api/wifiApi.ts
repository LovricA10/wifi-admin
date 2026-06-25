import axios, { AxiosError } from 'axios';
import { WifiConfiguration, ErrorBody } from '../types/wifi';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';

const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10_000,
});

function extractErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const body = error.response?.data as ErrorBody | undefined;
    if (body?.message) return body.message;
    if (error.code === 'ECONNABORTED') return 'Request timed out. Is the backend running?';
    if (!error.response) return 'Cannot reach the backend. Is it running on port 8081?';
    return `Backend returned ${error.response.status}`;
  }
  return 'An unexpected error occurred';
}

export async function getWifiConfiguration(cpeId: string): Promise<WifiConfiguration> {
  try {
    const { data } = await apiClient.get<WifiConfiguration>(
      `/wifi-parameter/${encodeURIComponent(cpeId)}`
    );
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error));
  }
}

export async function updateWifiConfiguration(config: WifiConfiguration): Promise<WifiConfiguration> {
  try {
    const { data } = await apiClient.put<WifiConfiguration>('/wifi-parameter', config);
    return data;
  } catch (error) {
    throw new Error(extractErrorMessage(error));
  }
}

export async function getHealthStatus(): Promise<{ status: string }> {
  try {
    const { data } = await apiClient.get<{ status: string }>('/actuator/health');
    return data;
  } catch {
    return { status: 'DOWN' };
  }
}
