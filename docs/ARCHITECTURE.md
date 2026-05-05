# Архитектура xservis.pro

```
                    ┌─────────────────────────────┐
                    │   Android Client (Kotlin)   │
                    │  Compose UI · Hilt · Xray   │
                    └──────────────┬──────────────┘
                                   │ HTTPS (JWT)
                                   ▼
                    ┌──────────────────────────────┐
                    │     api.xservis.pro          │
                    │   FastAPI · async SQLAlchemy │
                    └─┬────────┬──────────┬────────┘
                      │        │          │
              ┌───────▼───┐ ┌──▼──┐ ┌─────▼─────────┐
              │ MySQL 8.0 │ │Redis│ │ Payment APIs  │
              │ xservis_  │ │cache│ │ CryptoCloud   │
              │ prod      │ └─────┘ │ Lava · AAIO   │
              └─────┬─────┘         └───────────────┘
                    │
                    ▼
                ┌────────────────────────────┐
                │ admin.xservis.pro          │
                │  React · Vite · Leaflet    │
                └────────────────────────────┘

                    VPN-серверы:
                ┌────────────┐    ┌────────────┐
                │ vpn1 (RU)  │    │ vpn2 (DE)  │ ...
                │ Xray-core  │    │ Xray-core  │
                │ Reality    │    │ Reality    │
                │ AmneziaWG  │    │ AmneziaWG  │
                └────────────┘    └────────────┘
```

## Слои

### Android-клиент (`android/`)

- `pro.xservis.client.ui.MainActivity` — единственная Activity.
- `pro.xservis.client.ui.nav.RootNavGraph` — нижняя навигация и 4 таба.
- `pro.xservis.client.ui.screens.home.HomeScreen` — большая кнопка подключения,
  карточка сканера, статистика.
- `pro.xservis.client.data.scan.NetworkScanner` — 9 проб (DNS, TCP, TLS,
  SNI, HTTP-trace, throttle test, ASN, гео). Возвращает `ScanResult` с
  enum `BlockMethod` и рекомендованным протоколом.
- `pro.xservis.client.vpn.XservisVpnService` — `VpnService` с foreground-нотификацией.
  В iter 2 интегрируется libv2ray.aar.

### Backend (`backend/`)

- `xservis_backend.main` — FastAPI app, CORS, роуты.
- `routes/auth.py` — `/v1/auth/register|refresh` (телефон / e-mail / device-id).
- `routes/telemetry.py` — `/v1/telemetry/scan` принимает скан, добавляет ASN/ISP/Geo,
  обновляет `isp_block_methods`.
- `routes/configs.py` — `/v1/configs/active` и `/v1/configs/qr` генерируют
  vless://, ss://, trojan://, amneziawg:// под нужный протокол.
- `routes/payments.py` — `/v1/payments/create`, webhook'и от провайдеров.
- `routes/admin.py` — admin-only эндпоинты.

### База данных (`sql/`)

- `001_init_schema.sql` — создание схемы с нуля (включая новые таблицы `telemetry_scans`,
  `isp_block_methods`, `auth_tokens`, `clients.phone/email/role`).
- `002_migrate_legacy.sql` — `INSERT … ON DUPLICATE KEY UPDATE` из легаси-схемы
  `xvpn_legacy` в `xservis_prod` для всех ~600 пользователей.

### Admin-панель (`admin/`)

- React 18 + Vite + TypeScript.
- Тёмная тема (CSS-переменные в `styles.css`).
- 5 страниц: Dashboard, Telemetry, BlockMap, Users, Payments.
- API проксируется на `localhost:8000` в dev, на `api.xservis.pro` в prod.

## Поток данных при сканировании

```
[Android]                           [Backend]                       [DB]
─────────                           ─────────                       ───
NetworkScanner.scan()
  │ (9 probes)
  │ classify() ─► ScanResult        
  │
  ├─► UI обновляется (HomeViewModel)
  │
  └─► POST /v1/telemetry/scan ───►  geo.lookup_ip(client_ip)
                                    │ (MaxMind GeoIP)
                                    ▼
                                    INSERT telemetry_scans ───►  ✓
                                    │
                                    INSERT/UPDATE
                                    isp_block_methods       ───►  ✓
                                    │
                                    ◄─── 200 OK
                                          {detected_isp, country}

[Admin]
─────────
GET /v1/admin/blocks/map  ───►  SELECT isp_block_methods
                                    ◄─── points[]
[Admin UI]
─────────
BlockMap.tsx рендерит точки
```

## Деплой на xservis.pro (iter 2)

```bash
# 1. На VPS:
ssh root@xservis.pro

# 2. nginx + сертификаты
apt install nginx certbot python3-certbot-nginx
certbot --nginx -d api.xservis.pro -d admin.xservis.pro

# 3. Backend
cd /srv/xservis
git clone https://github.com/ai1991arz-png/xservis.git .
cd backend && python3.11 -m venv .venv && .venv/bin/pip install -e .
cp .env.example .env  # заполнить ключами
mysql < ../sql/001_init_schema.sql
mysql < ../sql/002_migrate_legacy.sql

# 4. systemd
cat >/etc/systemd/system/xservis-backend.service <<EOF
[Unit]
Description=xservis backend
After=network.target

[Service]
WorkingDirectory=/srv/xservis/backend
EnvironmentFile=/srv/xservis/backend/.env
ExecStart=/srv/xservis/backend/.venv/bin/uvicorn xservis_backend.main:app --host 127.0.0.1 --port 8000
Restart=always

[Install]
WantedBy=multi-user.target
EOF
systemctl enable --now xservis-backend

# 5. Admin
cd /srv/xservis/admin && npm ci && npm run build
# обслуживаем dist/ из nginx на admin.xservis.pro
```

## Безопасность развёртывания

- HTTPS-only (TLS 1.2+).
- CORS-allowlist в `CORS_ALLOWED_ORIGINS`.
- JWT секрет (`API_SECRET`) — длинная случайная строка из `/dev/urandom`.
- DB-юзер с минимальными правами (нет DDL).
- Webhook-эндпоинты валидируют HMAC-подпись провайдера.
- VpnService требует `BIND_VPN_SERVICE`, недоступную сторонним приложениям.
