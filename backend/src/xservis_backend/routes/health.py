"""Health check + version endpoint."""

from __future__ import annotations

from fastapi import APIRouter

from .. import __version__

router = APIRouter()


@router.get("/healthz", tags=["health"])
async def healthz() -> dict[str, str]:
    return {"status": "ok", "version": __version__}


@router.get("/", tags=["health"])
async def root() -> dict[str, str]:
    return {"name": "xservis backend", "version": __version__}
