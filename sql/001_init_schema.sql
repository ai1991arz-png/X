-- xservis.pro — fresh schema (MySQL 8.0+)
-- This script creates the database from scratch. To migrate from the legacy
-- Telegram-bot system, run 002_migrate_legacy.sql AFTER this script and AFTER
-- importing the legacy `xvpn_prod` dump into a transient schema.

CREATE DATABASE IF NOT EXISTS `xservis_prod`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `xservis_prod`;

CREATE TABLE IF NOT EXISTS `clients` (
  `user_id`           BIGINT       NOT NULL,
  `public_key`        VARCHAR(36)  NULL,
  `phone`             VARCHAR(32)  NULL,
  `email`             VARCHAR(160) NULL,
  `password_hash`     VARCHAR(255) NULL,
  `role`              VARCHAR(16)  NOT NULL DEFAULT 'user',
  `active_until`      DATETIME     NULL,
  `invited_by`        BIGINT       NULL,
  `referral_credits`  INT          NOT NULL DEFAULT 0,
  `notified`          TINYINT      NOT NULL DEFAULT 0,
  `balance`           DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `partner_balance`   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `partner_code`      VARCHAR(32)  NULL,
  `server_id`         INT          NULL,
  `created_at`        TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
  `registered_at`     DATETIME     NULL,
  `device_limit`      INT          NOT NULL DEFAULT 1,
  `device_allowance`  INT          NOT NULL DEFAULT 0,
  `replaced_until`    DATETIME     NULL,
  `replaced_uuid`     VARCHAR(255) NULL,
  `vip`               TINYINT(1)   NOT NULL DEFAULT 0,
  `sid`               VARCHAR(32)  NULL,
  PRIMARY KEY (`user_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_email` (`email`),
  KEY `idx_active_until` (`active_until`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `servers` (
  `id`            INT          NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(50)  NOT NULL,
  `address`       VARCHAR(100) NOT NULL,
  `port`          INT          NOT NULL,
  `vision_port`   INT          NULL,
  `uuid`          VARCHAR(36)  NOT NULL,
  `transport`     VARCHAR(10)  DEFAULT 'tcp',
  `path`          VARCHAR(255) DEFAULT '',
  `tls`           TINYINT(1)   DEFAULT 1,
  `sni`           VARCHAR(255) DEFAULT '',
  `user_limit`    INT          DEFAULT 100,
  `current_users` INT          DEFAULT 0,
  `domain`        VARCHAR(255) NULL,
  `role`          ENUM('main','reserve') DEFAULT 'main',
  `reality_pbk`   VARCHAR(64)  NULL,
  `reality_sid`   VARCHAR(32)  NOT NULL DEFAULT '',
  `reality_port`  INT          NOT NULL DEFAULT 8443,
  `reality_sni`   VARCHAR(255) NOT NULL DEFAULT 'apple.com',
  `priority`      TINYINT      NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_role_priority` (`role`,`priority`,`id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `devices` (
  `id`            INT          NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `uuid`          VARCHAR(100) NOT NULL,
  `active_until`  DATETIME     NOT NULL,
  `server_id`     INT          NOT NULL,
  `created_at`    TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_uuid` (`uuid`),
  KEY `idx_devices_user_active` (`user_id`,`active_until`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `payments` (
  `payment_id`     VARCHAR(84)  NOT NULL,
  `user_id`        BIGINT       NOT NULL,
  `amount_cents`   BIGINT       NOT NULL,
  `currency`       CHAR(3)      NOT NULL,
  `status`         ENUM('pending','succeeded','failed','canceled','refunded') NOT NULL,
  `provider`       VARCHAR(32)  NULL,
  `plan`           VARCHAR(64)  NULL,
  `period_months`  TINYINT      NULL,
  `paid_at`        DATETIME     NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`payment_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_paid_at` (`paid_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `referrals` (
  `inviter_id`   BIGINT NOT NULL,
  `invitee_id`   BIGINT NOT NULL,
  `activated`    TINYINT(1) DEFAULT 0,
  `activated_at` DATETIME NULL,
  PRIMARY KEY (`inviter_id`,`invitee_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `partner_referrals` (
  `partner_id` BIGINT NOT NULL,
  `client_id`  BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`partner_id`,`client_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `partner_accruals` (
  `id`                BIGINT NOT NULL AUTO_INCREMENT,
  `partner_id`        BIGINT NOT NULL,
  `user_id`           BIGINT NOT NULL,
  `payment_id`        VARCHAR(84) NULL,
  `kind`              ENUM('free_activation','first_paid','renewal') NOT NULL,
  `percent_bp`        INT NOT NULL,
  `base_amount_cents` BIGINT NOT NULL,
  `accrual_cents`     BIGINT NOT NULL,
  `note`              VARCHAR(255) NULL,
  `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_partner_payment` (`partner_id`,`payment_id`),
  KEY `idx_partner_created` (`partner_id`,`created_at`)
) ENGINE=InnoDB;

-- ===== New tables (post-Telegram era) =====

CREATE TABLE IF NOT EXISTS `telemetry_scans` (
  `id`                   BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`              BIGINT NOT NULL,
  `device_uuid`          VARCHAR(64) NULL,
  `created_at`           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ip_address`           VARCHAR(64) NULL,
  `asn`                  INT NULL,
  `isp`                  VARCHAR(128) NULL,
  `country`              VARCHAR(8) NULL,
  `region`               VARCHAR(128) NULL,
  `city`                 VARCHAR(128) NULL,
  `lat`                  DOUBLE NULL,
  `lon`                  DOUBLE NULL,
  `block_method`         VARCHAR(32) NOT NULL,
  `recommended_protocol` VARCHAR(64) NOT NULL,
  `readiness_score`      INT NOT NULL DEFAULT 0,
  `raw_probes`           JSON NULL,
  `raw_user_agent`       VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`,`created_at`),
  KEY `idx_isp` (`isp`),
  KEY `idx_country_region` (`country`,`region`),
  KEY `idx_block_method` (`block_method`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `isp_block_methods` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `asn`           INT NULL,
  `isp`           VARCHAR(128) NULL,
  `country`       VARCHAR(8) NULL,
  `region`        VARCHAR(128) NULL,
  `block_method`  VARCHAR(32) NOT NULL,
  `sample_count`  INT NOT NULL DEFAULT 1,
  `last_seen_at`  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_isp_method` (`asn`,`isp`,`region`,`block_method`),
  KEY `idx_country` (`country`),
  KEY `idx_method` (`block_method`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `auth_tokens` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT NOT NULL,
  `token_hash` VARCHAR(128) NOT NULL,
  `issued_at`  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME NOT NULL,
  `revoked_at` DATETIME NULL,
  `user_agent` VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_token_hash` (`token_hash`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB;
