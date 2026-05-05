"""Authentication routes — phone / email / device-id flows."""

from __future__ import annotations

import secrets
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Request, status
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import Client
from ..security import create_access_token, create_refresh_token, decode_token

router = APIRouter()


class RegisterIn(BaseModel):
    phone: str | None = Field(default=None, max_length=32)
    email: str | None = Field(default=None, max_length=160)
    device_id: str = Field(..., min_length=8, max_length=128)


class TokenOut(BaseModel):
    access_token: str
    refresh_token: str
    user_id: int
    token_type: str = "Bearer"


@router.post("/register", response_model=TokenOut)
async def register(
    body: RegisterIn,
    session: Annotated[AsyncSession, Depends(get_session)],
    request: Request,
) -> TokenOut:
    """Create new user (or restore existing one matched by phone/email)."""
    existing: Client | None = None
    if body.phone:
        existing = (await session.execute(select(Client).where(Client.phone == body.phone))).scalar_one_or_none()
    if existing is None and body.email:
        existing = (await session.execute(select(Client).where(Client.email == body.email))).scalar_one_or_none()

    if existing is None:
        # Generate a synthetic user_id stable per device (legacy schema uses BIGINT)
        new_id = secrets.randbits(48)
        client = Client(
            user_id=new_id,
            phone=body.phone,
            email=body.email,
            sid=body.device_id[:32],
            role="user",
        )
        session.add(client)
        await session.commit()
        existing = client

    return TokenOut(
        access_token=create_access_token(existing.user_id, role=existing.role),
        refresh_token=create_refresh_token(existing.user_id),
        user_id=existing.user_id,
    )


class RefreshIn(BaseModel):
    refresh_token: str


@router.post("/refresh", response_model=TokenOut)
async def refresh(
    body: RefreshIn,
    session: Annotated[AsyncSession, Depends(get_session)],
) -> TokenOut:
    claims = decode_token(body.refresh_token)
    if claims.get("typ") != "refresh":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="not a refresh token")
    user_id = int(claims["sub"])
    client = await session.get(Client, user_id)
    if not client:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="user not found")
    return TokenOut(
        access_token=create_access_token(client.user_id, role=client.role),
        refresh_token=create_refresh_token(client.user_id),
        user_id=client.user_id,
    )
