"""Admin panel endpoints — guarded by JWT role=admin."""

from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy import desc, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import Client, IspBlockMethod, Payment, TelemetryScan
from ..security import require_admin

router = APIRouter()


class StatsOut(BaseModel):
    total_users: int
    active_users: int
    paying_users: int
    total_scans: int
    total_revenue_rub: int


@router.get("/stats", response_model=StatsOut)
async def stats(
    session: Annotated[AsyncSession, Depends(get_session)],
    _: Annotated[dict[str, Any], Depends(require_admin)],
) -> StatsOut:
    total_users = (await session.execute(select(func.count(Client.user_id)))).scalar() or 0
    active_users = (
        await session.execute(
            select(func.count(Client.user_id)).where(Client.active_until > func.now())
        )
    ).scalar() or 0
    paying_users = (
        await session.execute(
            select(func.count(func.distinct(Payment.user_id))).where(Payment.status == "succeeded")
        )
    ).scalar() or 0
    total_scans = (await session.execute(select(func.count(TelemetryScan.id)))).scalar() or 0
    total_revenue = (
        await session.execute(
            select(func.coalesce(func.sum(Payment.amount_cents), 0)).where(Payment.status == "succeeded")
        )
    ).scalar() or 0
    return StatsOut(
        total_users=total_users,
        active_users=active_users,
        paying_users=paying_users,
        total_scans=total_scans,
        total_revenue_rub=total_revenue // 100,
    )


class UserOut(BaseModel):
    user_id: int
    phone: str | None
    email: str | None
    active_until: str | None
    server_id: int | None
    balance: float
    role: str


@router.get("/users", response_model=list[UserOut])
async def list_users(
    session: Annotated[AsyncSession, Depends(get_session)],
    _: Annotated[dict[str, Any], Depends(require_admin)],
    limit: int = Query(default=100, le=1000),
    offset: int = 0,
) -> list[UserOut]:
    rows = (
        await session.execute(select(Client).order_by(desc(Client.created_at)).offset(offset).limit(limit))
    ).scalars()
    return [
        UserOut(
            user_id=c.user_id,
            phone=c.phone,
            email=c.email,
            active_until=c.active_until.isoformat() if c.active_until else None,
            server_id=c.server_id,
            balance=float(c.balance),
            role=c.role,
        )
        for c in rows
    ]


class BlockMapPoint(BaseModel):
    asn: int | None
    isp: str | None
    country: str | None
    region: str | None
    block_method: str
    sample_count: int


@router.get("/blocks/map", response_model=list[BlockMapPoint])
async def block_map(
    session: Annotated[AsyncSession, Depends(get_session)],
    _: Annotated[dict[str, Any], Depends(require_admin)],
) -> list[BlockMapPoint]:
    rows = (
        await session.execute(
            select(IspBlockMethod).order_by(desc(IspBlockMethod.sample_count)).limit(2000)
        )
    ).scalars()
    return [
        BlockMapPoint(
            asn=r.asn,
            isp=r.isp,
            country=r.country,
            region=r.region,
            block_method=r.block_method,
            sample_count=r.sample_count,
        )
        for r in rows
    ]


class TelemetryRow(BaseModel):
    id: int
    user_id: int
    created_at: str
    ip_address: str | None
    isp: str | None
    country: str | None
    region: str | None
    city: str | None
    block_method: str
    recommended_protocol: str
    readiness_score: int


@router.get("/telemetry", response_model=list[TelemetryRow])
async def list_telemetry(
    session: Annotated[AsyncSession, Depends(get_session)],
    _: Annotated[dict[str, Any], Depends(require_admin)],
    limit: int = Query(default=200, le=1000),
    offset: int = 0,
) -> list[TelemetryRow]:
    rows = (
        await session.execute(
            select(TelemetryScan).order_by(desc(TelemetryScan.created_at)).offset(offset).limit(limit)
        )
    ).scalars()
    return [
        TelemetryRow(
            id=s.id,
            user_id=s.user_id,
            created_at=s.created_at.isoformat() if s.created_at else "",
            ip_address=s.ip_address,
            isp=s.isp,
            country=s.country,
            region=s.region,
            city=s.city,
            block_method=s.block_method,
            recommended_protocol=s.recommended_protocol,
            readiness_score=s.readiness_score,
        )
        for s in rows
    ]
