"""Application configuration loaded from environment / .env file."""

from __future__ import annotations

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    db_dsn: str = Field(default="mysql+asyncmy://root:@127.0.0.1:3306/xservis_dev", alias="DB_DSN")

    api_secret: str = Field(default="dev-only-change-me", alias="API_SECRET")
    jwt_algo: str = Field(default="HS256", alias="JWT_ALGO")
    access_token_ttl_min: int = Field(default=60, alias="ACCESS_TOKEN_TTL_MIN")
    refresh_token_ttl_days: int = Field(default=30, alias="REFRESH_TOKEN_TTL_DAYS")

    cors_allowed_origins: str = Field(
        default="https://xservis.pro,https://admin.xservis.pro,http://localhost:5173",
        alias="CORS_ALLOWED_ORIGINS",
    )

    cryptocloud_api_key: str = Field(default="", alias="CRYPTOCLOUD_API_KEY")
    cryptocloud_shop_id: str = Field(default="", alias="CRYPTOCLOUD_SHOP_ID")
    cryptocloud_secret: str = Field(default="", alias="CRYPTOCLOUD_SECRET")

    lava_shop_id: str = Field(default="", alias="LAVA_SHOP_ID")
    lava_api_key: str = Field(default="", alias="LAVA_API_KEY")
    lava_secret: str = Field(default="", alias="LAVA_SECRET")

    aaio_merchant_id: str = Field(default="", alias="AAIO_MERCHANT_ID")
    aaio_api_key: str = Field(default="", alias="AAIO_API_KEY")
    aaio_secret: str = Field(default="", alias="AAIO_SECRET")

    geoip_db_path: str = Field(default="", alias="GEOIP_DB_PATH")

    @property
    def cors_origins(self) -> list[str]:
        return [o.strip() for o in self.cors_allowed_origins.split(",") if o.strip()]


settings = Settings()
