# xservis backend

FastAPI + MySQL backend для приложения xservis.

## Эндпоинты

| Метод | Путь                              | Описание                                      |
|-------|-----------------------------------|-----------------------------------------------|
| POST  | `/v1/auth/register`               | Создание/восстановление аккаунта (телефон, email или device-id) |
| POST  | `/v1/auth/refresh`                | Обновление JWT                                |
| POST  | `/v1/telemetry/scan`              | Приём результатов сканирования сети           |
| GET   | `/v1/telemetry/me`                | История сканирований текущего пользователя    |
| GET   | `/v1/configs/active`              | Конфиг для подключения (Reality / WG / SS-2022) |
| GET   | `/v1/configs/qr`                  | QR-код для импорта в любой клиент             |
| GET   | `/v1/servers`                     | Список доступных серверов                     |
| POST  | `/v1/payments/create`             | Создание счёта (СБП / крипта)                 |
| POST  | `/v1/payments/webhook/cryptocloud`| Webhook от CryptoCloud                        |
| POST  | `/v1/payments/webhook/lava`       | Webhook от Lava.top                           |
| POST  | `/v1/payments/webhook/aaio`       | Webhook от AAIO                               |
| GET   | `/v1/admin/users`                 | Список пользователей (требует `admin` JWT)    |
| GET   | `/v1/admin/blocks/map`            | GeoJSON карта блокировок по регионам/ISP      |
| GET   | `/v1/admin/stats`                 | Сводная статистика                            |

## Запуск локально

```bash
pip install -e ".[dev]"
cp .env.example .env
# Заполните DB_DSN и API_SECRET
alembic upgrade head
uvicorn xservis_backend.main:app --reload --host 0.0.0.0 --port 8000
```

## Деплой на VPS (xservis.pro)

См. `scripts/deploy.sh` и `docs/DEPLOY.md`.
