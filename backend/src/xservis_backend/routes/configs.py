"""VPN config & QR generation."""

from __future__ import annotations

import io
from typing import Annotated, Any

import qrcode
from fastapi import APIRouter, Depends, Query
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import Client, Server
from ..security import require_user
from ..services.config_generator import build_config_link

router = APIRouter()


class ConfigOut(BaseModel):
    server_id: int
    server_name: str
    protocol: str
    link: str  # vless://, ss://, wg://, or amneziawg://
    json_config: dict | None = None


@router.get("/active", response_model=ConfigOut)
async def active_config(
    session: Annotated[AsyncSession, Depends(get_session)],
    claims: Annotated[dict[str, Any], Depends(require_user)],
    protocol: str = Query(default="reality"),
) -> ConfigOut:
    user_id = int(claims["sub"])
    client = await session.get(Client, user_id)
    server = (
        await session.execute(select(Server).order_by(Server.priority.asc()).limit(1))
    ).scalar_one_or_none()
    if server is None:
        # Fallback (no servers configured yet)
        return ConfigOut(
            server_id=0,
            server_name="bootstrap",
            protocol=protocol,
            link="vless://placeholder@xservis.pro:443?type=tcp&security=reality#xservis-bootstrap",
        )
    link = build_config_link(server, client, protocol=protocol)
    return ConfigOut(server_id=server.id, server_name=server.name, protocol=protocol, link=link)


@router.get("/qr")
async def qr_code(
    session: Annotated[AsyncSession, Depends(get_session)],
    claims: Annotated[dict[str, Any], Depends(require_user)],
    protocol: str = Query(default="reality"),
) -> StreamingResponse:
    cfg = await active_config(session, claims, protocol)
    img = qrcode.make(cfg.link)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)
    return StreamingResponse(buf, media_type="image/png")
