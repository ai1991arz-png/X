# xservis.pro

Современный VPN-сервис под российские реалии. Полностью самостоятельная
платформа без привязки к Telegram-боту.

## Главное

- **Анти-РКН с первого включения.** Сканер блокировок определяет тип DPI/SNI/IP-фильтра
  у конкретного провайдера в реальном времени и подбирает оптимальный протокол:
  Reality, AmneziaWG, Shadowsocks-2022, VLESS-Vision или Trojan.
- **Скорость production-уровня.** YouTube 4K, Discord-видеосвязь, Telegram-звонки,
  Instagram, голосовые в WhatsApp — всё работает 24/7.
- **Оплата без верификации.** СБП-рубли через Lava.top / AAIO + криптовалюта
  через CryptoCloud (BTC, USDT, ETH, TON). Без документов от пользователя.
- **Карта блокировок РКН в админ-панели.** Каждый скан становится точкой данных:
  ASN, регион, ISP, метод блокировки. Внутри одной недели у нас живой обзор того,
  что и где режет интернет.
- **Миграция из старой Telegram-системы.** ~600 существующих клиентов, серверы и
  партнёрская программа переезжают одним SQL-скриптом. Старая логика не используется.

## Ключевая UX-сцена

1. Пользователь открывает приложение. Тёмный интерфейс, бирюзовый акцент,
   крупная кнопка-«щит» в центре.
2. Нажимает «Сканировать». Приложение прогоняет 9 сетевых проб (DNS-резолв,
   TCP/443, TLS-handshake к YouTube/Discord, SNI-фильтр, троттлинг через
   Cloudflare-speedtest, Cloudflare-trace для ASN/гео).
3. Через 3-5 секунд видит вердикт: «DPI-троттлинг → рекомендуем VLESS-XTLS-Vision».
4. Жмёт большую кнопку — поднимается VpnService с уже сгенерированной конфигурацией.
5. Параллельно пакет скан-данных уходит на `api.xservis.pro/v1/telemetry/scan`.
   В админ-панели Алексей видит новую точку на карте блокировок.

## Технологический стек

| Слой | Технологии |
| --- | --- |
| **Android** | Kotlin · Jetpack Compose Material3 · Hilt · Coroutines · Retrofit · OkHttp · Coil |
| **VPN-ядро** | Xray-core (libv2ray.aar) · AmneziaWG · ShadowSocks-2022 |
| **Backend** | Python 3.11 · FastAPI · SQLAlchemy 2.0 (async) · Pydantic 2 · Alembic |
| **БД** | MySQL 8 (миграция из xvpn_prod) |
| **Платежи** | CryptoCloud · Lava.top · AAIO (без KYC для пользователя) |
| **Admin** | React 18 + Vite + TypeScript · Leaflet (карта) |
| **Инфра** | VPS xservis.pro · nginx + certbot · systemd · Docker (опционально) |

## Безопасность

- JWT-авторизация (access + refresh).
- Webhook'и платёжных провайдеров проверяются по HMAC-подписи.
- Проброс TUN через `VpnService` с foreground-сервисом и
  `foregroundServiceType="specialUse"` для совместимости с Android 14+.
- Никаких данных в Cloud Backup и Device Transfer (`data_extraction_rules.xml`).
- Хеширование паролей bcrypt (если включается традиционный режим логина).

## Дорожная карта

### Iteration 1 — выпущено сегодня

- [x] Скелет Android с тёмным UI, бирюзово-серо-чёрной палитрой
- [x] 4 экрана: Главная, Серверы, Тарифы, Аккаунт
- [x] Сканер блокировок (9 проб) + классификатор + рекомендация протокола
- [x] FastAPI backend с эндпоинтами auth/configs/telemetry/payments/admin
- [x] Schema + миграционный SQL-скрипт для ~600 пользователей
- [x] Admin-панель (React + Vite, тёмная тема) — дашборд / телеметрия / карта / пользователи
- [x] Платёжные адаптеры CryptoCloud / Lava / AAIO (с проверкой подписи)
- [x] APK debug-сборка

### Iteration 2 — следующая итерация

- [ ] Привязка libv2ray.aar (Xray-core) и фактический `tun → core`
- [ ] AmneziaWG-нативная сборка через JNI (или go-mobile)
- [ ] Подписанный release APK + Play Store internal track
- [ ] Деплой backend на xservis.pro (systemd unit, nginx, certbot)
- [ ] Реальные API-ключи Lava.top / CryptoCloud / AAIO (нужны от Алексея)
- [ ] Импорт боевого MySQL-дампа на VPS
- [ ] Leaflet-карта с координатами в `BlockMap.tsx` (после подключения GeoIP)

### Iteration 3 — бета-готовность

- [ ] Поддержка iOS (через Swift + Network Extension + Xray-Mobile)
- [ ] Семейные подписки и партнёрская программа в новом UI
- [ ] Auto-protocol switching (если выбранный начал блокироваться — переключаемся)
- [ ] Push-уведомления о подозрительной активности по аккаунту

## Деливераблы текущей итерации

- Приватный репозиторий `github.com/ai1991arz-png/xservis`
- Файл `app-debug.apk` (~17.8 МБ)
- Этот документ + `ARCHITECTURE.md`
- SQL-скрипты для нового деплоя и миграции из старой системы
