import io
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from aegis_agent.identity import AgentIdentity


class FakeRuntime:
    def __init__(self):
        self.reported_at = None
        self.started = False

    def run_once(self, reported_at: str) -> AgentIdentity:
        self.reported_at = reported_at
        return AgentIdentity("agt_cli", "host_cli", "secret-from-runtime")

    def run_forever(self, reported_at_provider) -> AgentIdentity:
        self.started = True
        self.reported_at = reported_at_provider()
        return AgentIdentity("agt_cli", "host_cli", "secret-from-runtime")


class AgentCliTest(unittest.TestCase):
    def test_run_once_command_prints_registered_identity_summary(self):
        from aegis_agent.cli import main

        runtime = FakeRuntime()
        output = io.StringIO()

        with tempfile.TemporaryDirectory() as tmp:
            config_path = Path(tmp) / "agent.yml"
            config_path.write_text(
                "\n".join(
                    [
                        "server_url: http://127.0.0.1:8080/api",
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

            exit_code = main(
                [
                    "run-once",
                    "--config",
                    str(config_path),
                    "--reported-at",
                    "2026-06-04T17:30:00+08:00",
                ],
                runtime_factory=lambda config: runtime,
                stdout=output,
            )

        self.assertEqual(0, exit_code)
        self.assertEqual("2026-06-04T17:30:00+08:00", runtime.reported_at)
        self.assertEqual(
            "run-once completed: agentId=agt_cli hostId=host_cli\n",
            output.getvalue(),
        )

    def test_run_command_starts_continuous_runtime(self):
        from aegis_agent.cli import main

        runtime = FakeRuntime()
        output = io.StringIO()

        with tempfile.TemporaryDirectory() as tmp:
            config_path = Path(tmp) / "agent.yml"
            config_path.write_text(
                "\n".join(
                    [
                        "server_url: http://127.0.0.1:8080/api",
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

            exit_code = main(
                [
                    "run",
                    "--config",
                    str(config_path),
                ],
                runtime_factory=lambda config: runtime,
                stdout=output,
            )

        self.assertEqual(0, exit_code)
        self.assertTrue(runtime.started)
        self.assertIsNotNone(runtime.reported_at)
        self.assertEqual(
            "agent started\nagent stopped: agentId=agt_cli hostId=host_cli\n",
            output.getvalue(),
        )

    def test_windows_run_once_script_exposes_cli_help(self):
        agent_dir = Path(__file__).resolve().parents[1]
        env = os.environ.copy()
        env["AEGIS_AGENT_PYTHON"] = sys.executable

        completed = subprocess.run(
            ["cmd", "/c", str(agent_dir / "run-once.cmd"), "--help"],
            cwd=agent_dir.parent,
            env=env,
            capture_output=True,
            text=False,
            check=False,
        )

        self.assertEqual(0, completed.returncode)
        stdout = completed.stdout.decode("utf-8", errors="replace")
        self.assertIn("usage: aegis-agent run-once", stdout)

    def test_windows_run_script_exposes_cli_help(self):
        agent_dir = Path(__file__).resolve().parents[1]
        env = os.environ.copy()
        env["AEGIS_AGENT_PYTHON"] = sys.executable

        completed = subprocess.run(
            ["cmd", "/c", str(agent_dir / "run.cmd"), "--help"],
            cwd=agent_dir.parent,
            env=env,
            capture_output=True,
            text=False,
            check=False,
        )

        self.assertEqual(0, completed.returncode)
        stdout = completed.stdout.decode("utf-8", errors="replace")
        self.assertIn("usage: aegis-agent run", stdout)


if __name__ == "__main__":
    unittest.main()
