from dataclasses import dataclass

from aegis_agent.config import AgentConfig
from aegis_agent.http_client import post_json
from aegis_agent.identity import AgentIdentity


@dataclass(frozen=True)
class CpuMetric:
    usage_percent: float
    per_core_usage_percent: list[float]


@dataclass(frozen=True)
class MemoryMetric:
    total_bytes: int
    used_bytes: int
    available_bytes: int
    usage_percent: float


@dataclass(frozen=True)
class DiskMetric:
    mount_point: str
    total_bytes: int
    used_bytes: int
    free_bytes: int
    usage_percent: float


@dataclass(frozen=True)
class NetworkMetric:
    interface_name: str
    bytes_sent: int
    bytes_received: int
    send_rate_bytes_per_second: float
    receive_rate_bytes_per_second: float


@dataclass(frozen=True)
class TcpMetric:
    connection_count: int
    listening_ports: list[int]


@dataclass(frozen=True)
class HostMetricSnapshot:
    reported_at: str
    cpu: CpuMetric
    memory: MemoryMetric
    disks: list[DiskMetric]
    networks: list[NetworkMetric]
    tcp: TcpMetric


class HostMetricReporter:
    def __init__(self, config: AgentConfig):
        self._config = config

    def report(self, identity: AgentIdentity, snapshot: HostMetricSnapshot) -> None:
        post_json(
            f"{self._config.server_url}/metrics/host",
            _to_payload(identity, snapshot),
            {
                "X-Agent-Id": identity.agent_id,
                "X-Agent-Secret": identity.agent_secret,
            },
        )


def _to_payload(identity: AgentIdentity, snapshot: HostMetricSnapshot) -> dict:
    return {
        "agentId": identity.agent_id,
        "hostId": identity.host_id,
        "reportedAt": snapshot.reported_at,
        "cpu": {
            "usagePercent": snapshot.cpu.usage_percent,
            "perCoreUsagePercent": snapshot.cpu.per_core_usage_percent,
        },
        "memory": {
            "totalBytes": snapshot.memory.total_bytes,
            "usedBytes": snapshot.memory.used_bytes,
            "availableBytes": snapshot.memory.available_bytes,
            "usagePercent": snapshot.memory.usage_percent,
        },
        "disks": [
            {
                "mountPoint": disk.mount_point,
                "totalBytes": disk.total_bytes,
                "usedBytes": disk.used_bytes,
                "freeBytes": disk.free_bytes,
                "usagePercent": disk.usage_percent,
            }
            for disk in snapshot.disks
        ],
        "networks": [
            {
                "interfaceName": network.interface_name,
                "bytesSent": network.bytes_sent,
                "bytesReceived": network.bytes_received,
                "sendRateBytesPerSecond": network.send_rate_bytes_per_second,
                "receiveRateBytesPerSecond": network.receive_rate_bytes_per_second,
            }
            for network in snapshot.networks
        ],
        "tcp": {
            "connectionCount": snapshot.tcp.connection_count,
            "listeningPorts": snapshot.tcp.listening_ports,
        },
    }
