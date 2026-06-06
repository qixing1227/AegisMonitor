import json
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

from aegis_agent.config import AgentConfig
from aegis_agent.host import HostSnapshot
from aegis_agent.identity import AgentIdentity, AgentStateStore
from aegis_agent.metrics import (
    CpuMetric,
    DiskMetric,
    HostMetricSnapshot,
    MemoryMetric,
    NetworkMetric,
    TcpMetric,
)
from aegis_agent.runtime import AgentRuntime
from aegis_agent.service_discovery import DiscoveredService, ServiceDiscoverySnapshot


class RuntimeHandler(BaseHTTPRequestHandler):
    requests = []

    def do_POST(self):
        body = self.rfile.read(int(self.headers["Content-Length"]))
        RuntimeHandler.requests.append(
            {
                "path": self.path,
                "headers": dict(self.headers),
                "body": json.loads(body.decode("utf-8")),
            }
        )

        if self.path == "/api/agents/register":
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
        else:
            response = {
                "success": True,
                "code": "OK",
                "message": "accepted",
                "data": {},
            }

        payload = json.dumps(response).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format, *args):
        return


class FakeMetricCollector:
    def collect(self, reported_at: str) -> HostMetricSnapshot:
        return HostMetricSnapshot(
            reported_at=reported_at,
            cpu=CpuMetric(usage_percent=42.5, per_core_usage_percent=[35.0, 50.0]),
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
                    bytes_sent=123456,
                    bytes_received=654321,
                    send_rate_bytes_per_second=2048,
                    receive_rate_bytes_per_second=4096,
                )
            ],
            tcp=TcpMetric(connection_count=128, listening_ports=[80, 3306, 6379, 8080]),
        )


class FakeServiceDiscoveryCollector:
    def collect(self, reported_at: str) -> ServiceDiscoverySnapshot:
        return ServiceDiscoverySnapshot(
            reported_at=reported_at,
            services=[
                DiscoveredService(
                    service_name="aegis-backend",
                    stack_type="SPRING_BOOT",
                    process_name="java.exe",
                    pid=10240,
                    ports=[8080],
                    status="RUNNING",
                    command_line="java -jar aegis-backend.jar",
                )
            ],
        )


class AgentRuntimeTest(unittest.TestCase):
    def test_first_run_registers_agent_then_reports_heartbeat_and_metrics(self):
        RuntimeHandler.requests = []
        server = HTTPServer(("127.0.0.1", 0), RuntimeHandler)
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

            identity = AgentRuntime(
                config=config,
                state_store=store,
                host_snapshot_provider=lambda: HostSnapshot(
                    hostname="DESKTOP-QIXING",
                    ip_address="192.168.1.10",
                    os_name="Windows 11",
                    os_version="10.0.22631",
                    cpu_cores=8,
                    memory_total_bytes=17179869184,
                    boot_time="2026-06-04T09:00:00+08:00",
                    agent_version="0.1.0",
                ),
                metric_collector=FakeMetricCollector(),
            ).run_once(reported_at="2026-06-04T17:30:00+08:00")

            self.assertEqual(
                identity,
                AgentIdentity("agt_001", "host_001", "generated-agent-secret"),
            )
            self.assertEqual(store.load(), identity)
            self.assertEqual(
                [request["path"] for request in RuntimeHandler.requests],
                [
                    "/api/agents/register",
                    "/api/agents/heartbeat",
                    "/api/metrics/host",
                ],
            )
            self.assertEqual(
                RuntimeHandler.requests[1]["headers"]["X-Agent-Secret"],
                "generated-agent-secret",
            )
            self.assertEqual(RuntimeHandler.requests[2]["body"]["tcp"]["connectionCount"], 128)

    def test_existing_identity_skips_registration_before_reporting(self):
        RuntimeHandler.requests = []
        server = HTTPServer(("127.0.0.1", 0), RuntimeHandler)
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
            existing_identity = AgentIdentity(
                "agt_existing",
                "host_existing",
                "existing-agent-secret",
            )
            store.save(existing_identity)

            identity = AgentRuntime(
                config=config,
                state_store=store,
                host_snapshot_provider=lambda: self.fail("host snapshot should not be collected"),
                metric_collector=FakeMetricCollector(),
            ).run_once(reported_at="2026-06-04T17:31:00+08:00")

            self.assertEqual(identity, existing_identity)
            self.assertEqual(
                [request["path"] for request in RuntimeHandler.requests],
                [
                    "/api/agents/heartbeat",
                    "/api/metrics/host",
                ],
            )
            self.assertEqual(RuntimeHandler.requests[0]["headers"]["X-Agent-Id"], "agt_existing")
            self.assertEqual(
                RuntimeHandler.requests[1]["body"]["agentId"],
                "agt_existing",
            )

    def test_run_once_reports_services_when_service_discovery_is_configured(self):
        RuntimeHandler.requests = []
        server = HTTPServer(("127.0.0.1", 0), RuntimeHandler)
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

            AgentRuntime(
                config=config,
                state_store=AgentStateStore(config.state_file),
                host_snapshot_provider=lambda: HostSnapshot(
                    hostname="DESKTOP-QIXING",
                    ip_address="192.168.1.10",
                    os_name="Windows 11",
                    os_version="10.0.22631",
                    cpu_cores=8,
                    memory_total_bytes=17179869184,
                    boot_time="2026-06-04T09:00:00+08:00",
                    agent_version="0.1.0",
                ),
                metric_collector=FakeMetricCollector(),
                service_discovery_collector=FakeServiceDiscoveryCollector(),
            ).run_once(reported_at="2026-06-04T17:35:00+08:00")

            self.assertEqual(
                [request["path"] for request in RuntimeHandler.requests],
                [
                    "/api/agents/register",
                    "/api/agents/heartbeat",
                    "/api/metrics/host",
                    "/api/services/report",
                ],
            )
            self.assertEqual(
                RuntimeHandler.requests[3]["body"]["services"][0]["stackType"],
                "SPRING_BOOT",
            )


if __name__ == "__main__":
    unittest.main()
