import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer

from aegis_agent.config import AgentConfig
from aegis_agent.identity import AgentIdentity
from aegis_agent.metrics import (
    CpuMetric,
    DiskMetric,
    HostMetricReporter,
    HostMetricSnapshot,
    MemoryMetric,
    NetworkMetric,
    TcpMetric,
)


class HostMetricHandler(BaseHTTPRequestHandler):
    captured = {}

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        HostMetricHandler.captured = {
            "path": self.path,
            "headers": dict(self.headers),
            "body": json.loads(body.decode("utf-8")),
        }
        payload = json.dumps(
            {
                "success": True,
                "code": "OK",
                "message": "metrics accepted",
                "data": {"written": True, "generatedAlertCount": 0},
            }
        ).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        return


class HostMetricReportTest(unittest.TestCase):
    def test_reports_host_metrics_using_agent_credentials(self):
        server = HTTPServer(("127.0.0.1", 0), HostMetricHandler)
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
        snapshot = HostMetricSnapshot(
            reported_at="2026-06-04T17:30:00+08:00",
            cpu=CpuMetric(usage_percent=42.5, per_core_usage_percent=[35.1, 47.2]),
            memory=MemoryMetric(
                total_bytes=17179869184,
                used_bytes=8589934592,
                available_bytes=8589934592,
                usage_percent=50.0,
            ),
            disks=[
                DiskMetric(
                    mount_point="C:",
                    total_bytes=536870912000,
                    used_bytes=322122547200,
                    free_bytes=214748364800,
                    usage_percent=60.0,
                )
            ],
            networks=[
                NetworkMetric(
                    interface_name="WLAN",
                    bytes_sent=123456789,
                    bytes_received=987654321,
                    send_rate_bytes_per_second=2048,
                    receive_rate_bytes_per_second=4096,
                )
            ],
            tcp=TcpMetric(connection_count=128, listening_ports=[80, 3306, 6379, 8080]),
        )

        HostMetricReporter(config).report(identity, snapshot)

        captured = HostMetricHandler.captured
        self.assertEqual(captured["path"], "/api/metrics/host")
        self.assertEqual(captured["headers"]["X-Agent-Id"], "agt_001")
        self.assertEqual(captured["headers"]["X-Agent-Secret"], "generated-agent-secret")
        self.assertEqual(captured["body"]["agentId"], "agt_001")
        self.assertEqual(captured["body"]["hostId"], "host_001")
        self.assertEqual(captured["body"]["cpu"]["usagePercent"], 42.5)
        self.assertEqual(captured["body"]["memory"]["usagePercent"], 50.0)
        self.assertEqual(captured["body"]["disks"][0]["mountPoint"], "C:")
        self.assertEqual(captured["body"]["networks"][0]["interfaceName"], "WLAN")
        self.assertEqual(captured["body"]["tcp"]["listeningPorts"], [80, 3306, 6379, 8080])


if __name__ == "__main__":
    unittest.main()
