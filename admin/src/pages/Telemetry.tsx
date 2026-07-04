import { useEffect, useState } from "react";
import { api, TelemetryRow } from "../api";

export default function Telemetry() {
  const [rows, setRows] = useState<TelemetryRow[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.telemetry().then(setRows).catch((e) => setError(String(e)));
  }, []);

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Телеметрия сканирований</h1>
      {error && <div className="card" style={{ borderColor: "var(--danger)" }}>{error}</div>}
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>Время</th>
              <th>Пользователь</th>
              <th>IP</th>
              <th>Провайдер / ASN</th>
              <th>Регион</th>
              <th>Блокировка</th>
              <th>Протокол</th>
              <th>Готовность</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>{new Date(r.created_at).toLocaleString("ru-RU")}</td>
                <td>{r.user_id}</td>
                <td>{r.ip_address ?? "—"}</td>
                <td>{r.isp ?? "—"}</td>
                <td>{[r.country, r.region, r.city].filter(Boolean).join(" / ") || "—"}</td>
                <td><span className={`method-pill ${pillTag(r.block_method)}`}>{r.block_method}</span></td>
                <td>{r.recommended_protocol}</td>
                <td>{r.readiness_score}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function pillTag(method: string): string {
  if (method === "None") return "tag-success";
  if (method === "FullBlock" || method === "IpBlock") return "tag-danger";
  return "tag-warning";
}
