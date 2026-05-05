import { NavLink, Route, Routes } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import Telemetry from "./pages/Telemetry";
import BlockMap from "./pages/BlockMap";
import Users from "./pages/Users";
import Payments from "./pages/Payments";

export default function App() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-logo">X</span>
          <span>xservis · admin</span>
        </div>
        <nav className="nav">
          <NavLink to="/" end>Дашборд</NavLink>
          <NavLink to="/telemetry">Телеметрия</NavLink>
          <NavLink to="/map">Карта блокировок</NavLink>
          <NavLink to="/users">Пользователи</NavLink>
          <NavLink to="/payments">Платежи</NavLink>
        </nav>
      </aside>
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/telemetry" element={<Telemetry />} />
          <Route path="/map" element={<BlockMap />} />
          <Route path="/users" element={<Users />} />
          <Route path="/payments" element={<Payments />} />
        </Routes>
      </main>
    </div>
  );
}
