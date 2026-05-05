"""Generate VPN config links for various protocols."""

from __future__ import annotations

import urllib.parse as up
import uuid as _uuid

from ..models import Client, Server


def build_config_link(server: Server, client: Client | None, protocol: str) -> str:
    user_uuid = (client.public_key if client and client.public_key else str(_uuid.uuid4()))
    proto = protocol.lower()

    if proto in ("reality", "vless-reality", "vless"):
        return _build_vless_reality(server, user_uuid)
    if proto in ("ss-2022", "shadowsocks", "ss"):
        return _build_shadowsocks_2022(server, user_uuid)
    if proto in ("trojan",):
        return _build_trojan(server, user_uuid)
    if proto in ("amneziawg", "wg"):
        return _build_amnezia_wg(server, user_uuid)
    return _build_vless_reality(server, user_uuid)


def _build_vless_reality(server: Server, user_uuid: str) -> str:
    params = {
        "type": server.transport or "tcp",
        "security": "reality",
        "pbk": server.reality_pbk or "",
        "sid": server.reality_sid or "",
        "sni": server.reality_sni or "www.cloudflare.com",
        "fp": "chrome",
        "flow": "xtls-rprx-vision",
    }
    if server.transport == "ws":
        params["path"] = server.path or "/ray"
    qs = up.urlencode({k: v for k, v in params.items() if v})
    label = up.quote(f"xservis · {server.name}")
    return f"vless://{user_uuid}@{server.address}:{server.reality_port}?{qs}#{label}"


def _build_shadowsocks_2022(server: Server, user_uuid: str) -> str:
    import base64

    method = "2022-blake3-aes-256-gcm"
    creds = f"{method}:{user_uuid}".encode()
    b64 = base64.urlsafe_b64encode(creds).rstrip(b"=").decode()
    label = up.quote(f"xservis · {server.name}")
    return f"ss://{b64}@{server.address}:{server.port}#{label}"


def _build_trojan(server: Server, user_uuid: str) -> str:
    params = {"sni": server.sni or server.domain or server.address, "type": "tcp"}
    qs = up.urlencode(params)
    label = up.quote(f"xservis · {server.name}")
    return f"trojan://{user_uuid}@{server.address}:{server.port}?{qs}#{label}"


def _build_amnezia_wg(server: Server, user_uuid: str) -> str:
    return (
        f"amneziawg://{user_uuid}@{server.address}:51820"
        f"?Jc=2&Jmin=10&Jmax=50&S1=70&S2=120#xservis-{server.name}"
    )
