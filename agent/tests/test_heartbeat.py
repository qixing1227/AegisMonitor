import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

from aegis_agent.config import AgentConfig
from aegis_agent.heartbeat import HeartbeatReporter
from aegis_agent.identity import AgentIdentity


class HeartbeatHandler(BaseHTTPRequestHandler):
    captured = {}

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        HeartbeatHandler.captured = {
            "path": self.path,
            "headers": dict(self.headers),
            "body": json.loads(body.decode("utf-8")),
        }
        payload = json.dumps(
            {
                "success": True,
                "code": "OK",
                "message": "heartbeat accepted",
                "data": {"serverTime": "2026-06-04T17:30:00+08:00"},
            }
        ).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        return


class HeartbeatTest(unittest.TestCase):
    def test_reports_heartbeat_with_registered_agent_credentials(self):
        server = HTTPServer(("127.0.0.1", 0), HeartbeatHandler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        self.addCleanup(thread.join, 1)
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)

        config = AgentConfig(
            server_url=f"http://127.0.0.1:{server.server_port}/api",
            register_token="demo-register-token",
            host_alias="demo-host-a",
            host_metric_interval_seconds=5,
            heartbeat_interval_seconds=10,
            service_discovery_interval_seconds=30,
            state_file="unused.json",
        )
        identity = AgentIdentity(
            agent_id="agt_001",
            host_id="host_001",
            agent_secret="generated-agent-secret",
        )

        HeartbeatReporter(config).report(
            identity,
            status="ONLINE",
            reported_at="2026-06-04T17:30:00+08:00",
        )

        self.assertEqual(HeartbeatHandler.captured["path"], "/api/agents/heartbeat")
        self.assertEqual(HeartbeatHandler.captured["headers"]["X-Agent-Id"], "agt_001")
        self.assertEqual(
            HeartbeatHandler.captured["headers"]["X-Agent-Secret"],
            "generated-agent-secret",
        )
        self.assertEqual(HeartbeatHandler.captured["body"]["agentId"], "agt_001")
        self.assertEqual(HeartbeatHandler.captured["body"]["hostId"], "host_001")
        self.assertEqual(HeartbeatHandler.captured["body"]["status"], "ONLINE")


if __name__ == "__main__":
    unittest.main()
