import { useEffect, useState } from "react";
import { api, User } from "../api";

export default function Users() {
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.users().then(setUsers).catch((e) => setError(String(e)));
  }, []);

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>Пользователи</h1>
      {error && <div className="card" style={{ borderColor: "var(--danger)" }}>{error}</div>}
      <div className="card">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Телефон / e-mail</th>
              <th>Подписка до</th>
              <th>Сервер</th>
              <th>Баланс</th>
              <th>Роль</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.user_id}>
                <td>{u.user_id}</td>
                <td>{u.phone ?? u.email ?? "—"}</td>
                <td>{u.active_until ? new Date(u.active_until).toLocaleDateString("ru-RU") : "—"}</td>
                <td>{u.server_id ?? "—"}</td>
                <td>{u.balance.toLocaleString("ru-RU")} ₽</td>
                <td>{u.role}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
