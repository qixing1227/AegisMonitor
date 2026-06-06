from aegis_agent.metrics import (
    CpuMetric,
    DiskMetric,
    HostMetricSnapshot,
    MemoryMetric,
    NetworkMetric,
    TcpMetric,
)


class PsutilMetricCollector:
    def __init__(self, psutil_provider):
        self._psutil = psutil_provider

    def collect(self, reported_at: str) -> HostMetricSnapshot:
        memory = self._psutil.virtual_memory()
        return HostMetricSnapshot(
            reported_at=reported_at,
            cpu=CpuMetric(
                usage_percent=float(self._psutil.cpu_percent(interval=None)),
                per_core_usage_percent=[
                    float(value) for value in self._psutil.cpu_percent(interval=None, percpu=True)
                ],
            ),
            memory=MemoryMetric(
                total_bytes=int(memory.total),
                used_bytes=int(memory.used),
                available_bytes=int(memory.available),
                usage_percent=float(memory.percent),
            ),
            disks=self._collect_disks(),
            networks=self._collect_networks(),
            tcp=self._collect_tcp(),
        )

    def _collect_disks(self) -> list[DiskMetric]:
        disks: list[DiskMetric] = []
        for partition in self._psutil.disk_partitions(all=False):
            usage = self._psutil.disk_usage(partition.mountpoint)
            disks.append(
                DiskMetric(
                    mount_point=partition.mountpoint,
                    total_bytes=int(usage.total),
                    used_bytes=int(usage.used),
                    free_bytes=int(usage.free),
                    usage_percent=float(usage.percent),
                )
            )
        return disks

    def _collect_networks(self) -> list[NetworkMetric]:
        networks: list[NetworkMetric] = []
        for interface_name, stats in self._psutil.net_io_counters(pernic=True).items():
            networks.append(
                NetworkMetric(
                    interface_name=interface_name,
                    bytes_sent=int(stats.bytes_sent),
                    bytes_received=int(stats.bytes_recv),
                    send_rate_bytes_per_second=0.0,
                    receive_rate_bytes_per_second=0.0,
                )
            )
        return networks

    def _collect_tcp(self) -> TcpMetric:
        connections = self._psutil.net_connections(kind="tcp")
        listening_ports = sorted(
            {
                int(connection.laddr[1])
                for connection in connections
                if connection.status == self._psutil.CONN_LISTEN and connection.laddr
            }
        )
        return TcpMetric(
            connection_count=len(connections),
            listening_ports=listening_ports,
        )


def create_default_metric_collector() -> PsutilMetricCollector:
    try:
        import psutil
    except ImportError as exc:
        raise RuntimeError("psutil is required for host metric collection") from exc
    return PsutilMetricCollector(psutil)
