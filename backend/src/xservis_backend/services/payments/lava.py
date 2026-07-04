"""Lava.top — SBP / Russian cards without KYC for the customer.

NB: Lava.top requires a merchant ID. Operating it commercially in Russia
requires the merchant to have a self-employed (самозанятый) or IP/OOO status.
End-users do NOT need to verify anything — they pay via SBP from any Russian bank.
"""

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

API_URL = "https://api.lava.top/business/invoice"


async def create_invoice(payment_id: str, amount_rub: int, plan: str, months: int) -> tuple[str, str, str]:
    if not settings.lava_api_key:
        url = f"https://xservis.pro/pay/demo/lava/{payment_id}"
        expires = (datetime.now(UTC) + timedelta(hours=1)).isoformat()
        return "lava", url, expires

    async with httpx.AsyncClient(timeout=10) as cli:
        resp = await cli.post(
            API_URL,
            headers={"X-Api-Key": settings.lava_api_key},
            json={
                "shopId": settings.lava_shop_id,
                "sum": float(amount_rub),
                "orderId": payment_id,
                "successUrl": "https://xservis.pro/pay/success",
                "failUrl": "https://xservis.pro/pay/fail",
                "comment": f"xservis {plan} ({months} мес.)",
                "includeService": ["sbp", "card"],
            },
        )
        resp.raise_for_status()
        data = resp.json()
    pay_url = data.get("data", {}).get("url") or data.get("url") or ""
    expires = (datetime.now(UTC) + timedelta(hours=1)).isoformat()
    return "lava", pay_url, expires


def verify_signature(payload: dict[str, Any], signature: str) -> bool:
    if not settings.lava_secret:
        return True
    body = "".join(f"{k}{v}" for k, v in sorted(payload.items()) if k != "signature")
    expected = hmac.new(settings.lava_secret.encode(), body.encode(), hashlib.sha256).hexdigest()
    return hmac.compare_digest(expected, signature)


async def handle_webhook(session: AsyncSession, payload: dict[str, Any]) -> None:
    payment_id = str(payload.get("order_id") or payload.get("orderId") or "")
    status = str(payload.get("status") or "").lower()
    if not payment_id or not status:
        return
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
