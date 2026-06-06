import argparse
from datetime import datetime, timezone
import sys
from typing import Callable, TextIO

from aegis_agent.config import AgentConfig, load_config
from aegis_agent.host_collector import collect_host_snapshot
from aegis_agent.identity import AgentStateStore
from aegis_agent.metric_collector import create_default_metric_collector
from aegis_agent.runtime import AgentRuntime
from aegis_agent.service_discovery import create_default_service_discovery_collector


AGENT_VERSION = "0.1.0"


def main(
    argv: list[str] | None = None,
    runtime_factory: Callable[[AgentConfig], AgentRuntime] | None = None,
    stdout: TextIO | None = None,
) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    output = stdout or sys.stdout

    if args.command == "run-once":
        config = load_config(args.config)
        factory = runtime_factory or create_default_runtime
        identity = factory(config).run_once(args.reported_at or _now_iso())
        _write(
            output,
            f"run-once completed: agentId={identity.agent_id} hostId={identity.host_id}\n",
        )
        return 0

    if args.command == "run":
        config = load_config(args.config)
        factory = runtime_factory or create_default_runtime
        _write(output, "agent started\n")

        try:
            identity = factory(config).run_forever(_now_iso)
        except KeyboardInterrupt:
            _write(output, "agent stopped\n")
            return 0

        if identity is not None:
            _write(
                output,
                f"agent stopped: agentId={identity.agent_id} hostId={identity.host_id}\n",
            )
        return 0

    parser.print_help(output)
    return 1


def create_default_runtime(config: AgentConfig) -> AgentRuntime:
    return AgentRuntime(
        config=config,
        state_store=AgentStateStore(config.state_file),
        host_snapshot_provider=lambda: collect_host_snapshot(AGENT_VERSION),
        metric_collector=create_default_metric_collector(),
        service_discovery_collector=create_default_service_discovery_collector(),
    )


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="aegis-agent")
    subparsers = parser.add_subparsers(dest="command")

    run_once = subparsers.add_parser("run-once")
    run_once.add_argument("--config", required=True)
    run_once.add_argument("--reported-at")

    run = subparsers.add_parser("run")
    run.add_argument("--config", required=True)

    return parser


def _now_iso() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def _write(output: TextIO, message: str) -> None:
    try:
        output.write(message)
        output.flush()
    except OSError:
        return


if __name__ == "__main__":
    raise SystemExit(main())
