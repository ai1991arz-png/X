"""Endpoint receiving block-method scans from the Android app."""

from __future__ import annotations

from typing import Annotated, Any

from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import IspBlockMethod, TelemetryScan
from ..security import require_user
from ..services.geo import lookup_ip

router = APIRouter()


class ScanProbe(BaseModel):
    id: str
    success: bool
    info: str | None = None


class ScanIn(BaseModel):
    device_uuid: str | None = Field(default=None, max_length=64)
    block_method: str = Field(..., max_length=32)
    recommended_protocol: str = Field(..., max_length=64)
    readiness_score: int = Field(..., ge=0, le=100)
    probes: list[ScanProbe] = Field(default_factory=list)
    user_agent: str | None = Field(default=None, max_length=255)


class ScanOut(BaseModel):
    id: int
    detected_isp: str | None
    detected_country: str | None
    detected_region: str | None
    server_time: str


@router.post("/scan", response_model=ScanOut)
async def submit_scan(
    body: ScanIn,
    request: Request,
    session: Annotated[AsyncSession, Depends(get_session)],
    claims: Annotated[dict[str, Any], Depends(require_user)],
) -> ScanOut:
    user_id = int(claims["sub"])
    client_ip = request.client.host if request.client else None
    geo = lookup_ip(client_ip) if client_ip else None

    scan = TelemetryScan(
        user_id=user_id,
        device_uuid=body.device_uuid,
        ip_address=client_ip,
        asn=geo.asn if geo else None,
        isp=geo.isp if geo else None,
        country=geo.country if geo else None,
        region=geo.region if geo else None,
        city=geo.city if geo else None,
        lat=geo.lat if geo else None,
        lon=geo.lon if geo else None,
        block_method=body.block_method,
        recommended_protocol=body.recommended_protocol,
        readiness_score=body.readiness_score,
        raw_probes={"probes": [p.model_dump() for p in body.probes]},
        raw_user_agent=body.user_agent,
    )
    session.add(scan)
    await session.flush()
    await session.refresh(scan, ["created_at"])

    # Update aggregated isp_block_methods (idempotent upsert-ish)
    if geo and geo.isp and body.block_method:
        existing = (
            await session.execute(
                select(IspBlockMethod).where(
                    IspBlockMethod.asn == geo.asn,
                    IspBlockMethod.isp == geo.isp,
                    IspBlockMethod.region == geo.region,
                    IspBlockMethod.block_method == body.block_method,
                )
            )
        ).scalar_one_or_none()
        if existing is None:
            session.add(
                IspBlockMethod(
                    asn=geo.asn,
                    isp=geo.isp,
                    country=geo.country,
                    region=geo.region,
                    block_method=body.block_method,
                    sample_count=1,
                )
            )
        else:
            existing.sample_count += 1

    await session.commit()
    return ScanOut(
        id=scan.id,
        detected_isp=geo.isp if geo else None,
        detected_country=geo.country if geo else None,
        detected_region=geo.region if geo else None,
        server_time=scan.created_at.isoformat() if scan.created_at else "",
    )
