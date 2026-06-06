import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

from aegis_agent.config import AgentConfig
from aegis_agent.identity import AgentIdentity
from aegis_agent.service_discovery import (
    DiscoveredService,
    ServiceDiscoveryReporter,
    ServiceDiscoverySnapshot,
)


class ServiceReportHandler(BaseHTTPRequestHandler):
    captured = {}

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        ServiceReportHandler.captured = {
            "path": self.path,
            "headers": dict(self.headers),
            "body": json.loads(body.decode("utf-8")),
        }
        payload = json.dumps(
            {
                "success": True,
                "code": "OK",
                "message": "services accepted",
                "data": {"upsertedCount": 2},
            }
        ).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        return


class ServiceReportTest(unittest.TestCase):
    def test_reports_discovered_services_using_agent_credentials(self):
        ServiceReportHandler.captured = {}
        server = HTTPServer(("127.0.0.1", 0), ServiceReportHandler)
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
        identity = AgentIdentity("agt_001", "host_001", "generated-agent-secret")
        snapshot = ServiceDiscoverySnapshot(
            reported_at="2026-06-04T17:35:00+08:00",
            services=[
                DiscoveredService(
                    service_name="aegis-backend",
                    stack_type="SPRING_BOOT",
                    process_name="java.exe",
                    pid=10240,
                    ports=[8080],
                    status="RUNNING",
                    command_line="java -jar aegis-backend.jar",
                ),
                DiscoveredService(
                    service_name="mysql",
                    stack_type="MYSQL",
                    process_name="mysqld.exe",
                    pid=3306,
                    ports=[3306],
                    status="RUNNING",
                    command_line="mysqld.exe",
                ),
            ],
        )

        ServiceDiscoveryReporter(config).report(identity, snapshot)

        captured = ServiceReportHandler.captured
        self.assertEqual(captured["path"], "/api/services/report")
        self.assertEqual(captured["headers"]["X-Agent-Id"], "agt_001")
        self.assertEqual(captured["headers"]["X-Agent-Secret"], "generated-agent-secret")
        self.assertEqual(captured["body"]["agentId"], "agt_001")
        self.assertEqual(captured["body"]["hostId"], "host_001")
        self.assertEqual(captured["body"]["reportedAt"], "2026-06-04T17:35:00+08:00")
        self.assertEqual(captured["body"]["services"][0]["stackType"], "SPRING_BOOT")
        self.assertEqual(captured["body"]["services"][0]["serviceName"], "aegis-backend")
        self.assertEqual(captured["body"]["services"][0]["ports"], [8080])
        self.assertEqual(captured["body"]["services"][1]["processName"], "mysqld.exe")


if __name__ == "__main__":
    unittest.main()
