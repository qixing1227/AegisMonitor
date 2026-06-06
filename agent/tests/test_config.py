import tempfile
import unittest
from pathlib import Path

from aegis_agent.config import load_config


class AgentConfigTest(unittest.TestCase):
    def test_loads_agent_config_from_flat_yaml_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            config_path = Path(tmp) / "agent.yml"
            config_path.write_text(
                "\n".join(
                    [
                        "server_url: http://localhost:8080/api",
                        "register_token: demo-register-token",
                        "host_alias: demo-host-a",
                        "host_metric_interval_seconds: 5",
                        "heartbeat_interval_seconds: 10",
                        "service_discovery_interval_seconds: 30",
                        "state_file: .agent-state.json",
                    ]
                ),
                encoding="utf-8",
            )

            config = load_config(config_path)

            self.assertEqual(config.server_url, "http://localhost:8080/api")
            self.assertEqual(config.register_token, "demo-register-token")
            self.assertEqual(config.host_alias, "demo-host-a")
            self.assertEqual(config.host_metric_interval_seconds, 5)
            self.assertEqual(config.heartbeat_interval_seconds, 10)
            self.assertEqual(config.service_discovery_interval_seconds, 30)
            self.assertEqual(config.state_file, Path(tmp) / ".agent-state.json")

    def test_repository_example_config_can_be_loaded_for_demo(self):
        config_path = Path(__file__).resolve().parents[1] / "agent.example.yml"

        config = load_config(config_path)

        self.assertEqual(config.server_url, "http://localhost:8080/api")
        self.assertEqual(config.register_token, "demo-register-token")
        self.assertEqual(config.host_alias, "demo-host-a")
        self.assertEqual(config.host_metric_interval_seconds, 5)
        self.assertEqual(config.heartbeat_interval_seconds, 10)
        self.assertEqual(config.service_discovery_interval_seconds, 30)
        self.assertEqual(config.state_file, config_path.parent / ".agent-state.json")


if __name__ == "__main__":
    unittest.main()
