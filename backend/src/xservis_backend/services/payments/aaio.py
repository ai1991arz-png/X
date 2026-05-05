"""AAIO — broad payment aggregator (SBP, Yoomoney, cards, USDT)."""

from __future__ import annotations

import hashlib
from datetime import UTC, datetime, timedelta

from sqlalchemy import update
from sqlalchemy.ext.asyncio import AsyncSession

from ...config import settings
from ...models import Client, Payment


def verify_signature(form: dict[str, str]) -> bool:
    if not settings.aaio_secret:
        return True
    sign = form.get("sign", "")
    parts = [
        settings.aaio_merchant_id,
        form.get("amount", ""),
        settings.aaio_secret,
        form.get("order_id", ""),
    ]
    expected = hashlib.sha256(":".join(parts).encode()).hexdigest()
    return expected.lower() == sign.lower()


async def handle_webhook(session: AsyncSession, form: dict[str, str]) -> None:
    payment_id = form.get("order_id") or ""
    status = (form.get("status") or "").lower()
    new_status = "succeeded" if status in ("success", "paid") else "failed"
    paid_at = datetime.now(UTC) if new_status == "succeeded" else None
    await session.execute(
        update(Payment).where(Payment.payment_id == payment_id).values(status=new_status, paid_at=paid_at)
    )
    if new_status == "succeeded":
        payment = await session.get(Payment, payment_id)
        if payment:
            client = await session.get(Client, payment.user_id)
            if client and payment.period_months:
                base = client.active_until or datetime.now(UTC)
                if base < datetime.now(UTC):
                    base = datetime.now(UTC)
                client.active_until = base + timedelta(days=30 * payment.period_months)
    await session.commit()
