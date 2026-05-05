# xservis.pro

Полностью самостоятельная Anti-RKN VPN-платформа. Без какой-либо привязки к Telegram-боту.

## Состав репозитория

| Каталог    | Назначение                                                                 |
|------------|-----------------------------------------------------------------------------|
| `android/` | Android-клиент (Kotlin + Jetpack Compose, тёмная тема бирюзовый/серый/чёрный). VPN-ядро на Xray-core (VLESS-Reality, Trojan, Shadowsocks-2022) + AmneziaWG. Сканер блокировок РКН, генератор конфигов, телеметрия. |
| `backend/` | FastAPI + MySQL backend. Авторизация, выдача конфигов, приём телеметрии, платежи (CryptoCloud + Lava + AAIO), партнёрская программа. |
| `admin/`   | React + Vite admin-панель (тёмная тема). Карта блокировок, агрегаты по ISP, управление серверами, выручка, реферальная программа. |
| `sql/`     | Схема БД и скрипт миграции существующих ~600 пользователей из старой Telegram-системы. |
| `scripts/` | Утилиты: генератор Reality-ключей, бэкап БД, sync-скрипты, deploy. |
| `docs/`    | Архитектура, презентация, дорожная карта, спецификация API. |

## Быстрый старт

См. `docs/ARCHITECTURE.md` для архитектуры и `docs/PRESENTATION.md` для презентации.

- Backend: `cd backend && pip install -e . && uvicorn xservis_backend.main:app --reload`
- Admin: `cd admin && npm install && npm run dev`
- Android: `cd android && ./gradlew :app:assembleDebug`

## Лицензия

Проприетарное ПО. © 2026 xservis.pro
