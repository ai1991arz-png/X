"""ORM models. Mirrors the legacy `xvpn_prod` schema where applicable
and adds new tables for telemetry / blocking-map analytics.
"""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    JSON,
    BigInteger,
    Boolean,
    DateTime,
    Enum,
    ForeignKey,
    Integer,
    Numeric,
    String,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column

from .db import Base


class Client(Base):
    __tablename__ = "clients"

    user_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    public_key: Mapped[str | None] = mapped_column(String(36))
    active_until: Mapped[datetime | None] = mapped_column(DateTime)
    invited_by: Mapped[int | None] = mapped_column(BigInteger)
    referral_credits: Mapped[int] = mapped_column(Integer, default=0)
    notified: Mapped[int] = mapped_column(Integer, default=0)
    balance: Mapped[Decimal] = mapped_column(Numeric(10, 2), default=Decimal("0.00"))
    partner_balance: Mapped[Decimal] = mapped_column(Numeric(10, 2), default=Decimal("0.00"))
    partner_code: Mapped[str | None] = mapped_column(String(32))
    server_id: Mapped[int | None] = mapped_column(Integer)
    created_at: Mapped[datetime | None] = mapped_column(DateTime, server_default=func.now())
    registered_at: Mapped[datetime | None] = mapped_column(DateTime)
    device_limit: Mapped[int] = mapped_column(Integer, default=1)
    device_allowance: Mapped[int] = mapped_column(Integer, default=0)
    replaced_until: Mapped[datetime | None] = mapped_column(DateTime)
    replaced_uuid: Mapped[str | None] = mapped_column(String(255))
    vip: Mapped[bool] = mapped_column(Boolean, default=False)
    sid: Mapped[str | None] = mapped_column(String(32))

    # New fields (for the post-Telegram era)
    phone: Mapped[str | None] = mapped_column(String(32))
    email: Mapped[str | None] = mapped_column(String(160))
    password_hash: Mapped[str | None] = mapped_column(String(255))
    role: Mapped[str] = mapped_column(String(16), default="user")  # user | admin


class Device(Base):
    __tablename__ = "devices"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, index=True)
    uuid: Mapped[str] = mapped_column(String(100), unique=True)
    active_until: Mapped[datetime] = mapped_column(DateTime)
    server_id: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class Server(Base):
    __tablename__ = "servers"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(50))
    address: Mapped[str] = mapped_column(String(100))
    port: Mapped[int] = mapped_column(Integer)
    vision_port: Mapped[int | None] = mapped_column(Integer)
    uuid: Mapped[str] = mapped_column(String(36))
    transport: Mapped[str] = mapped_column(String(10), default="tcp")
    path: Mapped[str] = mapped_column(String(255), default="")
    tls: Mapped[bool] = mapped_column(Boolean, default=True)
    sni: Mapped[str] = mapped_column(String(255), default="")
    user_limit: Mapped[int] = mapped_column(Integer, default=100)
    current_users: Mapped[int] = mapped_column(Integer, default=0)
    domain: Mapped[str | None] = mapped_column(String(255))
    role: Mapped[str] = mapped_column(Enum("main", "reserve"), default="main")
    reality_pbk: Mapped[str | None] = mapped_column(String(64))
    reality_sid: Mapped[str] = mapped_column(String(32), default="")
    reality_port: Mapped[int] = mapped_column(Integer, default=8443)
    reality_sni: Mapped[str] = mapped_column(String(255), default="apple.com")
    priority: Mapped[int] = mapped_column(Integer, default=1)


class Payment(Base):
    __tablename__ = "payments"

    payment_id: Mapped[str] = mapped_column(String(84), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger, index=True)
    amount_cents: Mapped[int] = mapped_column(BigInteger)
    currency: Mapped[str] = mapped_column(String(3))
    status: Mapped[str] = mapped_column(
        Enum("pending", "succeeded", "failed", "canceled", "refunded"),
    )
    provider: Mapped[str | None] = mapped_column(String(32))
    plan: Mapped[str | None] = mapped_column(String(64))
    period_months: Mapped[int | None] = mapped_column(Integer)
    paid_at: Mapped[datetime | None] = mapped_column(DateTime)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class TelemetryScan(Base):
    """A single network-block scan submitted by an Android client."""

    __tablename__ = "telemetry_scans"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("clients.user_id"), index=True)
    device_uuid: Mapped[str | None] = mapped_column(String(64), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), index=True)

    # Network identity (from server side, derived from request)
    ip_address: Mapped[str | None] = mapped_column(String(64), index=True)
    asn: Mapped[int | None] = mapped_column(Integer, index=True)
    isp: Mapped[str | None] = mapped_column(String(128), index=True)
    country: Mapped[str | None] = mapped_column(String(8), index=True)
    region: Mapped[str | None] = mapped_column(String(128), index=True)
    city: Mapped[str | None] = mapped_column(String(128), index=True)
    lat: Mapped[float | None] = mapped_column()
    lon: Mapped[float | None] = mapped_column()

    # Block fingerprint
    block_method: Mapped[str] = mapped_column(String(32), index=True)
    recommended_protocol: Mapped[str] = mapped_column(String(64))
    readiness_score: Mapped[int] = mapped_column(Integer, default=0)

    raw_probes: Mapped[dict | None] = mapped_column(JSON)
    raw_user_agent: Mapped[str | None] = mapped_column(String(255))


class IspBlockMethod(Base):
    """Aggregated ISP x block-method stats, materialised periodically."""

    __tablename__ = "isp_block_methods"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    asn: Mapped[int | None] = mapped_column(Integer, index=True)
    isp: Mapped[str | None] = mapped_column(String(128), index=True)
    country: Mapped[str | None] = mapped_column(String(8), index=True)
    region: Mapped[str | None] = mapped_column(String(128), index=True)
    block_method: Mapped[str] = mapped_column(String(32), index=True)
    sample_count: Mapped[int] = mapped_column(Integer, default=1)
    last_seen_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class AuthToken(Base):
    """Refresh-token store (revocable)."""

    __tablename__ = "auth_tokens"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    user_id: Mapped[int] = mapped_column(BigInteger, index=True)
    token_hash: Mapped[str] = mapped_column(String(128), unique=True)
    issued_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    expires_at: Mapped[datetime] = mapped_column(DateTime)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime)
    user_agent: Mapped[str | None] = mapped_column(String(255))
