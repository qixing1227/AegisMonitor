from dataclasses import dataclass
import re

from aegis_agent.config import AgentConfig
from aegis_agent.http_client import post_json
from aegis_agent.identity import AgentIdentity


@dataclass(frozen=True)
class DiscoveredService:
    service_name: str
    stack_type: str
    process_name: str
    pid: int
    ports: list[int]
    status: str
    command_line: str


@dataclass(frozen=True)
class ServiceDiscoverySnapshot:
    reported_at: str
    services: list[DiscoveredService]


class PsutilServiceDiscoveryCollector:
    def __init__(self, psutil_provider):
        self._psutil = psutil_provider

    def collect(self, reported_at: str) -> ServiceDiscoverySnapshot:
        ports_by_pid = self._listening_ports_by_pid()
        services: list[DiscoveredService] = []

        for process in self._psutil.process_iter(["pid", "name", "cmdline"]):
            service = _identify_service(process.info, ports_by_pid.get(int(process.info["pid"]), []))
            if service is not None:
                services.append(service)

        return ServiceDiscoverySnapshot(reported_at=reported_at, services=services)

    def _listening_ports_by_pid(self) -> dict[int, list[int]]:
        ports_by_pid: dict[int, set[int]] = {}
        for connection in self._psutil.net_connections(kind="tcp"):
            if connection.status != self._psutil.CONN_LISTEN or not connection.laddr:
                continue
            ports_by_pid.setdefault(int(connection.pid), set()).add(_address_port(connection.laddr))
        return {pid: sorted(ports) for pid, ports in ports_by_pid.items()}


class ServiceDiscoveryReporter:
    def __init__(self, config: AgentConfig):
        self._config = config

    def report(self, identity: AgentIdentity, snapshot: ServiceDiscoverySnapshot) -> None:
        post_json(
            f"{self._config.server_url}/services/report",
            _to_payload(identity, snapshot),
            {
                "X-Agent-Id": identity.agent_id,
                "X-Agent-Secret": identity.agent_secret,
            },
        )


def create_default_service_discovery_collector() -> PsutilServiceDiscoveryCollector:
    try:
        import psutil
    except ImportError as exc:
        raise RuntimeError("psutil is required for service discovery") from exc
    return PsutilServiceDiscoveryCollector(psutil)


def _to_payload(identity: AgentIdentity, snapshot: ServiceDiscoverySnapshot) -> dict:
    return {
        "agentId": identity.agent_id,
        "hostId": identity.host_id,
        "reportedAt": snapshot.reported_at,
        "services": [
            {
                "serviceName": service.service_name,
                "stackType": service.stack_type,
                "processName": service.process_name,
                "pid": service.pid,
                "ports": service.ports,
                "status": service.status,
                "commandLine": service.command_line,
            }
            for service in snapshot.services
        ],
    }


def _identify_service(info: dict, ports: list[int]) -> DiscoveredService | None:
    pid = int(info["pid"])
    process_name = str(info.get("name") or "")
    process_key = process_name.lower()
    cmdline = [str(part) for part in info.get("cmdline") or []]
    command_line = " ".join(cmdline)
    command_key = command_line.lower()

    if _is_spring_boot(process_key, command_key):
        return DiscoveredService(
            service_name=_java_service_name(cmdline),
            stack_type="SPRING_BOOT",
            process_name=process_name,
            pid=pid,
            ports=ports,
            status="RUNNING",
            command_line=command_line,
        )
    if "mysqld" in process_key:
        return _service("mysql", "MYSQL", process_name, pid, ports, command_line)
    if "redis-server" in process_key:
        return _service("redis", "REDIS", process_name, pid, ports, command_line)
    if "nginx" in process_key:
        return _service("nginx", "NGINX", process_name, pid, ports, command_line)
    if process_key in {"node", "node.exe"}:
        return _service(_node_service_name(cmdline), "NODEJS", process_name, pid, ports, command_line)
    return None


def _service(
    service_name: str,
    stack_type: str,
    process_name: str,
    pid: int,
    ports: list[int],
    command_line: str,
) -> DiscoveredService:
    return DiscoveredService(
        service_name=service_name,
        stack_type=stack_type,
        process_name=process_name,
        pid=pid,
        ports=ports,
        status="RUNNING",
        command_line=command_line,
    )


def _is_spring_boot(process_key: str, command_key: str) -> bool:
    return process_key in {"java", "java.exe"} and (
        "spring" in command_key or ".jar" in command_key
    )


def _java_service_name(cmdline: list[str]) -> str:
    for part in cmdline:
        if part.lower().endswith(".jar"):
            return _stem(part)
    return "spring-boot-app"


def _node_service_name(cmdline: list[str]) -> str:
    for part in cmdline[1:]:
        if part.endswith(".js"):
            return _stem(part)
    return "nodejs-app"


def _stem(path: str) -> str:
    file_name = re.split(r"[\\/]", path)[-1]
    return re.sub(r"\.[^.]+$", "", file_name)


def _address_port(address) -> int:
    port = getattr(address, "port", None)
    if port is not None:
        return int(port)
    return int(address[1])
