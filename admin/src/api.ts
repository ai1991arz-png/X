export interface Stats {
  total_users: number;
  active_users: number;
  paying_users: number;
  total_scans: number;
  total_revenue_rub: number;
}

export interface User {
  user_id: number;
  phone: string | null;
  email: string | null;
  active_until: string | null;
  server_id: number | null;
  balance: number;
  role: string;
}

export interface TelemetryRow {
  id: number;
  user_id: number;
  created_at: string;
  ip_address: string | null;
  isp: string | null;
  country: string | null;
  region: string | null;
  city: string | null;
  block_method: string;
  recommended_protocol: string;
  readiness_score: number;
}

export interface BlockMapPoint {
  asn: number | null;
  isp: string | null;
  country: string | null;
  region: string | null;
  block_method: string;
  sample_count: number;
}

const TOKEN_KEY = "xservis_admin_token";

function authHeaders(): HeadersInit {
  const t = localStorage.getItem(TOKEN_KEY) || "";
  return t ? { Authorization: `Bearer ${t}` } : {};
}

async function request<T>(path: string): Promise<T> {
  const resp = await fetch(path, { headers: authHeaders() });
  if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
  return resp.json() as Promise<T>;
}

export const api = {
  stats: () => request<Stats>("/v1/admin/stats"),
  users: (offset = 0, limit = 100) => request<User[]>(`/v1/admin/users?offset=${offset}&limit=${limit}`),
  telemetry: (offset = 0, limit = 200) =>
    request<TelemetryRow[]>(`/v1/admin/telemetry?offset=${offset}&limit=${limit}`),
  blockMap: () => request<BlockMapPoint[]>("/v1/admin/blocks/map"),
};
