"""IP -> ASN/ISP/geo lookup. Uses MaxMind if available, else falls back to a stub."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from ..config import settings


@dataclass
class GeoInfo:
    asn: int | None
    isp: str | None
    country: str | None
    region: str | None
    city: str | None
    lat: float | None
    lon: float | None


_reader = None


def _load_reader() -> None:
    """Lazily init MaxMind GeoIP reader if a database is configured."""
    global _reader
    if _reader is not None:
        return
    db_path = settings.geoip_db_path
    if not db_path or not Path(db_path).is_file():
        return
    try:
        import geoip2.database

        _reader = geoip2.database.Reader(db_path)
    except Exception:
        _reader = None


def lookup_ip(ip: str | None) -> GeoInfo | None:
    if not ip:
        return None
    _load_reader()
    if _reader is None:
        return GeoInfo(asn=None, isp=None, country=None, region=None, city=None, lat=None, lon=None)
    try:
        rec = _reader.city(ip)
        return GeoInfo(
            asn=getattr(getattr(rec, "traits", None), "autonomous_system_number", None),
            isp=getattr(getattr(rec, "traits", None), "isp", None)
            or getattr(getattr(rec, "traits", None), "autonomous_system_organization", None),
            country=rec.country.iso_code,
            region=rec.subdivisions.most_specific.name if rec.subdivisions else None,
            city=rec.city.name,
            lat=rec.location.latitude,
            lon=rec.location.longitude,
        )
    except Exception:
        return None
