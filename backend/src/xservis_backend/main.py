"""Application entry point."""

from __future__ import annotations

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .config import settings
from .routes import admin, auth, configs, health, payments, telemetry


def create_app() -> FastAPI:
    app = FastAPI(
        title="xservis backend",
        version="0.1.0",
        description="Anti-RKN VPN service: auth, telemetry, configs, payments, admin.",
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    app.include_router(health.router)
    app.include_router(auth.router, prefix="/v1/auth", tags=["auth"])
    app.include_router(telemetry.router, prefix="/v1/telemetry", tags=["telemetry"])
    app.include_router(configs.router, prefix="/v1/configs", tags=["configs"])
    app.include_router(payments.router, prefix="/v1/payments", tags=["payments"])
    app.include_router(admin.router, prefix="/v1/admin", tags=["admin"])

    return app


app = create_app()


def run() -> None:
    import uvicorn

    uvicorn.run("xservis_backend.main:app", host="0.0.0.0", port=8000, reload=False)
