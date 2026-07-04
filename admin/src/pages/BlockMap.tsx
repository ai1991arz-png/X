import { useEffect, useState } from "react";
import { api, BlockMapPoint } from "../api";

export default function BlockMap() {
  const [points, setPoints] = useState<BlockMapPoint[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.blockMap().then(setPoints).catch((e) => setError(String(e)));
  }, []);

  // Aggregate by country/region for the legend
  const grouped = points.reduce<Record<string, BlockMapPoint[]>>((acc, p) => {
    const key = `${p.country ?? "??"} · ${p.region ?? "—"}`;
    (acc[key] ||= []).push(p);
    return acc;
  }, {});

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Карта блокировок (ISP × регион)</h1>
      {error && <div className="card" style={{ borderColor: "var(--danger)" }}>{error}</div>}
      <div className="card" style={{ marginBottom: 16 }}>
        <h3 style={{ marginTop: 0 }}>Сводка</h3>
        <p style={{ color: "var(--text-secondary)" }}>
          Источник — поле <code>isp_block_methods</code>, агрегаты по результатам клиентских сканов.
          Когда подключим GeoIP, на этом месте появится Leaflet-карта России / СНГ с heat-маркерами.
        </p>
      </div>
      <div className="cards-grid">
        {Object.entries(grouped).map(([key, list]) => (
          <div key={key} className="card">
            <div className="stat-label">{key}</div>
            <div style={{ marginTop: 6 }}>
              {list.map((p, i) => (
                <div key={i} style={{ display: "flex", justifyContent: "space-between", padding: "3px 0" }}>
                  <span>{p.isp ?? "ASN " + p.asn}</span>
                  <span className={`method-pill ${pillTag(p.block_method)}`}>
                    {p.block_method} · {p.sample_count}
                  </span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function pillTag(method: string): string {
  if (method === "None") return "tag-success";
  if (method === "FullBlock" || method === "IpBlock") return "tag-danger";
  return "tag-warning";
}
