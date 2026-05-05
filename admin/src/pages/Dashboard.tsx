import { useEffect, useState } from "react";
import { api, Stats } from "../api";

export default function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.stats().then(setStats).catch((e) => setError(String(e)));
  }, []);

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Дашборд</h1>
      {error && <div className="card" style={{ borderColor: "var(--danger)" }}>{error}</div>}
      <div className="cards-grid">
        <StatCard label="Всего пользователей" value={stats?.total_users ?? "—"} />
        <StatCard label="Активные" value={stats?.active_users ?? "—"} accent />
        <StatCard label="Платящие" value={stats?.paying_users ?? "—"} />
        <StatCard label="Сканов" value={stats?.total_scans ?? "—"} />
        <StatCard label="Доход (₽)" value={(stats?.total_revenue_rub ?? 0).toLocaleString("ru-RU")} accent />
      </div>
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Последние события</h3>
        <p style={{ color: "var(--text-secondary)" }}>
          Здесь будет лента: новые регистрации, платежи, скан-репорты, аномалии блокировок.
        </p>
      </div>
    </div>
  );
}

function StatCard({ label, value, accent }: { label: string; value: number | string; accent?: boolean }) {
  return (
    <div className="card">
      <div className="stat-label">{label}</div>
      <div className="stat-value" style={{ color: accent ? "var(--teal)" : "inherit" }}>{value}</div>
    </div>
  );
}
