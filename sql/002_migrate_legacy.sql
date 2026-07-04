-- Migrate users from the legacy `xvpn_prod` (Telegram-bot-era) database into
-- the new `xservis_prod` schema.
--
-- Usage:
--   1. Import the legacy dump into the source database (e.g. `xvpn_legacy`).
--   2. Apply 001_init_schema.sql to create `xservis_prod`.
--   3. Run this script: `mysql < 002_migrate_legacy.sql`
--
-- This is idempotent — re-running is safe: existing rows are preserved
-- and new rows from the legacy db are merged in.

SET @LEGACY := 'xvpn_legacy';

-- Clients (~600 users)
INSERT INTO xservis_prod.clients
    (user_id, public_key, active_until, invited_by, referral_credits, notified,
     balance, partner_balance, partner_code, server_id, created_at, registered_at,
     device_limit, device_allowance, replaced_until, replaced_uuid, vip, sid, role)
SELECT
    user_id, public_key, active_until, invited_by, referral_credits, notified,
    balance, partner_balance, partner_code, server_id, created_at, registered_at,
    device_limit, device_allowance, replaced_until, replaced_uuid, vip, sid, 'user'
FROM xvpn_legacy.clients src
ON DUPLICATE KEY UPDATE
    public_key       = VALUES(public_key),
    active_until     = GREATEST(IFNULL(xservis_prod.clients.active_until, '1970-01-01'),
                                IFNULL(VALUES(active_until), '1970-01-01')),
    balance          = VALUES(balance),
    partner_balance  = VALUES(partner_balance);

-- Devices
INSERT INTO xservis_prod.devices (user_id, uuid, active_until, server_id, created_at)
SELECT user_id, uuid, active_until, server_id, created_at
FROM xvpn_legacy.devices
ON DUPLICATE KEY UPDATE active_until = VALUES(active_until);

-- Servers (re-import, but we keep manual edits)
INSERT INTO xservis_prod.servers
    (id, name, address, port, vision_port, uuid, transport, path, tls, sni,
     user_limit, current_users, domain, role, reality_pbk, reality_sid,
     reality_port, reality_sni, priority)
SELECT
    id, name, address, port, vision_port, uuid, transport, path, tls, sni,
    user_limit, current_users, domain, role, reality_pbk, reality_sid,
    reality_port, reality_sni, priority
FROM xvpn_legacy.servers
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    address = VALUES(address),
    port = VALUES(port),
    reality_pbk = VALUES(reality_pbk);

-- Payments
INSERT INTO xservis_prod.payments
    (payment_id, user_id, amount_cents, currency, status, provider, plan,
     period_months, paid_at, created_at)
SELECT payment_id, user_id, amount_cents, currency, status, provider, plan,
       period_months, paid_at, created_at
FROM xvpn_legacy.payments
ON DUPLICATE KEY UPDATE status = VALUES(status), paid_at = VALUES(paid_at);

-- Referrals & partners
INSERT IGNORE INTO xservis_prod.referrals SELECT * FROM xvpn_legacy.referrals;
INSERT IGNORE INTO xservis_prod.partner_referrals SELECT * FROM xvpn_legacy.partner_referrals;
INSERT IGNORE INTO xservis_prod.partner_accruals
    (id, partner_id, user_id, payment_id, kind, percent_bp, base_amount_cents,
     accrual_cents, note, created_at)
SELECT id, partner_id, user_id, payment_id, kind, percent_bp, base_amount_cents,
       accrual_cents, note, created_at
FROM xvpn_legacy.partner_accruals;

-- Drop the legacy Telegram-only states
-- (campaign_*, started_users, user_states, user_languages — not needed in new system)

SELECT
    (SELECT COUNT(*) FROM xservis_prod.clients)  AS migrated_clients,
    (SELECT COUNT(*) FROM xservis_prod.devices)  AS migrated_devices,
    (SELECT COUNT(*) FROM xservis_prod.payments) AS migrated_payments,
    (SELECT COUNT(*) FROM xservis_prod.servers)  AS migrated_servers;
