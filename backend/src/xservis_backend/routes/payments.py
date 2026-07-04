"""Payment creation + provider webhooks."""

from __future__ import annotations

import secrets
from datetime import datetime
from typing import Annotated, Any

from fastapi import APIRouter, Depends, Header, HTTPException, Request
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession

from ..db import get_session
from ..models import Payment
from ..security import require_user
from ..services.payments import aaio, cryptocloud, lava

router = APIRouter()


class CreatePaymentIn(BaseModel):
    plan: str = Field(..., examples=["m1", "m3", "m6", "m12"])
    method: str = Field(..., examples=["sbp", "crypto"])
    amount_rub: int = Field(..., gt=0)


class CreatePaymentOut(BaseModel):
    payment_id: str
    pay_url: str
    provider: str
    expires_at: str


@router.post("/create", response_model=CreatePaymentOut)
async def create_payment(
    body: CreatePaymentIn,
    session: Annotated[AsyncSession, Depends(get_session)],
    claims: Annotated[dict[str, Any], Depends(require_user)],
) -> CreatePaymentOut:
    user_id = int(claims["sub"])
    payment_id = secrets.token_urlsafe(24)
    months = int(body.plan.lstrip("m"))
    amount_cents = body.amount_rub * 100

    if body.method == "sbp":
        provider, pay_url, expires_at = await lava.create_invoice(
            payment_id, body.amount_rub, body.plan, months
        )
    elif body.method == "crypto":
        provider, pay_url, expires_at = await cryptocloud.create_invoice(
            payment_id, body.amount_rub, body.plan, months
        )
    else:
        raise HTTPException(status_code=400, detail=f"unsupported method: {body.method}")

    session.add(
        Payment(
            payment_id=payment_id,
            user_id=user_id,
            amount_cents=amount_cents,
            currency="RUB",
            status="pending",
            provider=provider,
            plan=body.plan,
            period_months=months,
            created_at=datetime.utcnow(),
        )
    )
    await session.commit()
    return CreatePaymentOut(
        payment_id=payment_id,
        pay_url=pay_url,
        provider=provider,
        expires_at=expires_at,
    )


@router.post("/webhook/cryptocloud")
async def webhook_cryptocloud(
    request: Request,
    session: Annotated[AsyncSession, Depends(get_session)],
    x_signature: str = Header(default=""),
) -> dict[str, str]:
    payload = await request.json()
    if not cryptocloud.verify_signature(payload, x_signature):
        raise HTTPException(status_code=401, detail="bad signature")
    await cryptocloud.handle_webhook(session, payload)
    return {"status": "ok"}


@router.post("/webhook/lava")
async def webhook_lava(
    request: Request,
    session: Annotated[AsyncSession, Depends(get_session)],
    x_signature: str = Header(default=""),
) -> dict[str, str]:
    payload = await request.json()
    if not lava.verify_signature(payload, x_signature):
        raise HTTPException(status_code=401, detail="bad signature")
    await lava.handle_webhook(session, payload)
    return {"status": "ok"}


@router.post("/webhook/aaio")
async def webhook_aaio(
    request: Request,
    session: Annotated[AsyncSession, Depends(get_session)],
) -> dict[str, str]:
    form = await request.form()
    payload = {k: str(v) for k, v in form.items()}
    if not aaio.verify_signature(payload):
        raise HTTPException(status_code=401, detail="bad signature")
    await aaio.handle_webhook(session, payload)
    return {"status": "ok"}
