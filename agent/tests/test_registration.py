import json
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

from aegis_agent.config import AgentConfig
from aegis_agent.host import HostSnapshot
from aegis_agent.identity import AgentIdentity, AgentStateStore
from aegis_agent.registration import AgentRegistrar


class RegistrationHandler(BaseHTTPRequestHandler):
    captured = {}

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        RegistrationHandler.captured = {
            "path": self.path,
            "headers": dict(self.headers),
            "body": json.loads(body.decode("utf-8")),
        }
        response = {
            "success": True,
            "code": "OK",
            "message": "registered",
            "data": {
                "agentId": "agt_001",
                "hostId": "host_001",
                "agentSecret": "generated-agent-secret",
                "approved": True,
            },
        }
        payload = json.dumps(response).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        return


class AgentRegistrationTest(unittest.TestCase):
    def test_registers_agent_and_persists_returned_identity(self):
        server = HTTPServer(("127.0.0.1", 0), RegistrationHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        self.addCleanup(thread.join, 1)
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)

        with tempfile.TemporaryDirectory() as tmp:
            config = AgentConfig(
                server_url=f"http://127.0.0.1:{server.server_port}/api",
                register_token="demo-register-token",
                host_alias="demo-host-a",
                host_metric_interval_seconds=5,
                heartbeat_interval_seconds=10,
                service_discovery_interval_seconds=30,
                state_file=Path(tmp) / ".agent-state.json",
            )
            store = AgentStateStore(config.state_file)
            host = HostSnapshot(
                hostname="DESKTOP-QIXING",
                ip_address="192.168.1.10",
                os_name="Windows 11",
                os_version="10.0.22631",
                cpu_cores=8,
                memory_total_bytes=17179869184,
                boot_time="2026-06-04T09:00:00+08:00",
                agent_version="0.1.0",
            )

            identity = AgentRegistrar(config, store).register(host)

            self.assertEqual(
                identity,
                AgentIdentity(
                    agent_id="agt_001",
                    host_id="host_001",
                    agent_secret="generated-agent-secret",
                ),
            )
            self.assertEqual(store.load(), identity)
            self.assertEqual(RegistrationHandler.captured["path"], "/api/agents/register")
            self.assertEqual(
                RegistrationHandler.captured["headers"]["X-Agent-Register-Token"],
                "demo-register-token",
            )
            self.assertEqual(RegistrationHandler.captured["body"]["alias"], "demo-host-a")
            self.assertEqual(RegistrationHandler.captured["body"]["hostname"], "DESKTOP-QIXING")
            self.assertEqual(
                RegistrationHandler.captured["body"]["configSummary"]["heartbeatIntervalSeconds"],
                10,
            )


if __name__ == "__main__":
    unittest.main()
