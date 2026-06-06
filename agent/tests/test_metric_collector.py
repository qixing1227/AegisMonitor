import unittest

from aegis_agent.metric_collector import PsutilMetricCollector


class FakeDiskUsage:
    total = 1000
    used = 400
    free = 600
    percent = 40.0


class FakeNetworkStats:
    bytes_sent = 100
    bytes_recv = 300


class FakeConnection:
    def __init__(self, status, port):
        self.status = status
        self.laddr = ("127.0.0.1", port)


class FakeVirtualMemory:
    total = 2000
    used = 500
    available = 1500
    percent = 25.0


class FakePsutil:
    CONN_LISTEN = "LISTEN"

    def cpu_percent(self, interval=None, percpu=False):
        if percpu:
            return [10.0, 20.0]
        return 15.0

    def virtual_memory(self):
        return FakeVirtualMemory()

    def disk_partitions(self, all=False):
        return [type("Partition", (), {"mountpoint": "C:"})()]

    def disk_usage(self, mountpoint):
        return FakeDiskUsage()

    def net_io_counters(self, pernic=True):
        return {"WLAN": FakeNetworkStats()}

    def net_connections(self, kind="tcp"):
        return [
            FakeConnection(self.CONN_LISTEN, 80),
            FakeConnection("ESTABLISHED", 443),
            FakeConnection(self.CONN_LISTEN, 8080),
        ]


class MetricCollectorTest(unittest.TestCase):
    def test_collects_host_metric_snapshot_from_psutil_provider(self):
        collector = PsutilMetricCollector(FakePsutil())

        snapshot = collector.collect(reported_at="2026-06-04T17:30:00+08:00")

        self.assertEqual(snapshot.reported_at, "2026-06-04T17:30:00+08:00")
        self.assertEqual(snapshot.cpu.usage_percent, 15.0)
        self.assertEqual(snapshot.cpu.per_core_usage_percent, [10.0, 20.0])
        self.assertEqual(snapshot.memory.total_bytes, 2000)
        self.assertEqual(snapshot.memory.usage_percent, 25.0)
        self.assertEqual(snapshot.disks[0].mount_point, "C:")
        self.assertEqual(snapshot.disks[0].usage_percent, 40.0)
        self.assertEqual(snapshot.networks[0].interface_name, "WLAN")
        self.assertEqual(snapshot.networks[0].bytes_received, 300)
        self.assertEqual(snapshot.tcp.connection_count, 3)
        self.assertEqual(snapshot.tcp.listening_ports, [80, 8080])


if __name__ == "__main__":
    unittest.main()
