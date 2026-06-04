from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class AgentConfig:
    server_url: str
    register_token: str
    host_alias: str
    host_metric_interval_seconds: int
    heartbeat_interval_seconds: int
    service_discovery_interval_seconds: int
    state_file: Path


def load_config(path: str | Path) -> AgentConfig:
    config_path = Path(path)
    values = _read_flat_yaml(config_path)

    state_file = Path(values["state_file"])
    if not state_file.is_absolute():
        state_file = config_path.parent / state_file

    return AgentConfig(
        server_url=values["server_url"].rstrip("/"),
        register_token=values["register_token"],
        host_alias=values["host_alias"],
        host_metric_interval_seconds=int(values["host_metric_interval_seconds"]),
        heartbeat_interval_seconds=int(values["heartbeat_interval_seconds"]),
        service_discovery_interval_seconds=int(values["service_discovery_interval_seconds"]),
        state_file=state_file,
    )


def _read_flat_yaml(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if ":" not in line:
            raise ValueError(f"Invalid config line: {raw_line}")
        key, value = line.split(":", 1)
        values[key.strip()] = value.strip()
    return values
