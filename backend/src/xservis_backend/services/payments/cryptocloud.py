"""CryptoCloud — accept BTC, USDT, ETH, etc. without KYC for the customer."""

from __future__ import annotations

import hashlib
import hmac
from datetime import UTC, datetime, timedelta
from typing import Any

import httpx
from sqlalchemy import update
from sqlalchemy.ext.asyncio import AsyncSession

from ...config import settings
from ...models import Client, Payment

API_URL = "https://api.cryptocloud.plus/v1/invoice/create"


async def create_invoice(payment_id: str, amount_rub: int, plan: str, months: int) -> tuple[str, str, str]:
    if not settings.cryptocloud_api_key:
        # Demo / fallback URL; real provider call requires API key
        url = f"https://xservis.pro/pay/demo/cryptocloud/{payment_id}"
        expires = (datetime.now(UTC) + timedelta(hours=1)).isoformat()
        return "cryptocloud", url, expires

    async with httpx.AsyncClient(timeout=10) as cli:
        resp = await cli.post(
            API_URL,
            headers={"Authorization": f"Token {settings.cryptocloud_api_key}"},
            json={
                "shop_id": settings.cryptocloud_shop_id,
                "amount": amount_rub,
                "currency": "RUB",
                "order_id": payment_id,
                "description": f"xservis {plan} ({months} мес.)",
            },
        )
        resp.raise_for_status()
        data = resp.json()
    pay_url = data.get("pay_url") or data.get("link") or ""
    expires = (datetime.now(UTC) + timedelta(hours=1)).isoformat()
    return "cryptocloud", pay_url, expires


def verify_signature(payload: dict[str, Any], signature: str) -> bool:
    if not settings.cryptocloud_secret:
        return True  # disabled in dev
    body = "&".join(f"{k}={v}" for k, v in sorted(payload.items()) if k != "signature")
    expected = hmac.new(settings.cryptocloud_secret.encode(), body.encode(), hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected, signature)


async def handle_webhook(session: AsyncSession, payload: dict[str, Any]) -> None:
    payment_id = str(payload.get("order_id") or payload.get("invoice_id") or "")
    status = str(payload.get("status") or "").lower()
    if not payment_id or not status:
        return
    new_status = "succeeded" if status in ("paid", "success", "succeeded") else "failed"
    paid_at = datetime.now(UTC) if new_status == "succeeded" else None
    await session.execute(
        update(Payment)
        .where(Payment.payment_id == payment_id)
        .values(status=new_status, paid_at=paid_at)
    )
    if new_status == "succeeded":
        await _extend_subscription(session, payment_id)
    await session.commit()


async def _extend_subscription(session: AsyncSession, payment_id: str) -> None:
    payment = await session.get(Payment, payment_id)
    if not payment or not payment.period_months:
        return
    client = await session.get(Client, payment.user_id)
    if not client:
        return
    base = client.active_until or datetime.now(UTC)
    if base < datetime.now(UTC):
        base = datetime.now(UTC)
    client.active_until = base + timedelta(days=30 * payment.period_months)
