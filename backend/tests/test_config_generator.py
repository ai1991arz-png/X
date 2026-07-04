"""Verify config link generators produce valid VLESS / SS / Trojan / WG URIs."""

from xservis_backend.models import Client, Server
from xservis_backend.services.config_generator import build_config_link


def _server() -> Server:
    s = Server()
    s.id = 1
    s.name = "vpn1"
    s.address = "188.213.0.161"
    s.port = 443
    s.uuid = "00000000-0000-0000-0000-000000000000"
    s.transport = "tcp"
    s.path = ""
    s.tls = True
    s.sni = "apple.com"
    s.user_limit = 100
    s.current_users = 0
    s.domain = None
    s.role = "main"
    s.reality_pbk = "ABCxyz"
    s.reality_sid = "deadbeef"
    s.reality_port = 8443
    s.reality_sni = "www.cloudflare.com"
    s.priority = 1
    return s


def _client() -> Client:
    c = Client()
    c.user_id = 42
    c.public_key = "11111111-2222-3333-4444-555555555555"
    c.role = "user"
    return c


def test_vless_reality() -> None:
    link = build_config_link(_server(), _client(), "reality")
    assert link.startswith("vless://")
    assert "security=reality" in link
    assert "pbk=ABCxyz" in link


def test_shadowsocks() -> None:
    link = build_config_link(_server(), _client(), "ss-2022")
    assert link.startswith("ss://")


def test_trojan() -> None:
    link = build_config_link(_server(), _client(), "trojan")
    assert link.startswith("trojan://")


def test_amnezia_wg() -> None:
    link = build_config_link(_server(), _client(), "amneziawg")
    assert link.startswith("amneziawg://")
