from dataclasses import dataclass
import unittest

from aegis_agent.service_discovery import PsutilServiceDiscoveryCollector


@dataclass(frozen=True)
class FakeAddress:
    ip: str
    port: int

    def __getitem__(self, index):
        return (self.ip, self.port)[index]


@dataclass(frozen=True)
class FakeConnection:
    pid: int
    status: str
    laddr: FakeAddress


class FakeProcess:
    def __init__(self, pid: int, name: str, cmdline: list[str]):
        self.info = {
            "pid": pid,
            "name": name,
            "cmdline": cmdline,
        }


class FakePsutil:
    CONN_LISTEN = "LISTEN"

    def process_iter(self, attrs):
        return [
            FakeProcess(
                10240,
                "java.exe",
                ["java", "-jar", "aegis-backend.jar", "--spring.profiles.active=dev"],
            ),
            FakeProcess(3306, "mysqld.exe", ["mysqld.exe", "--defaults-file=my.ini"]),
            FakeProcess(6379, "redis-server.exe", ["redis-server.exe", "redis.conf"]),
            FakeProcess(80, "nginx.exe", ["nginx.exe"]),
            FakeProcess(3000, "node.exe", ["node.exe", "server.js"]),
            FakeProcess(5555, "notepad.exe", ["notepad.exe"]),
        ]

    def net_connections(self, kind):
        return [
            FakeConnection(10240, "LISTEN", FakeAddress("0.0.0.0", 8080)),
            FakeConnection(3306, "LISTEN", FakeAddress("0.0.0.0", 3306)),
            FakeConnection(6379, "LISTEN", FakeAddress("127.0.0.1", 6379)),
            FakeConnection(80, "LISTEN", FakeAddress("0.0.0.0", 80)),
            FakeConnection(3000, "LISTEN", FakeAddress("0.0.0.0", 3000)),
            FakeConnection(5555, "LISTEN", FakeAddress("0.0.0.0", 5555)),
        ]


class ServiceDiscoveryTest(unittest.TestCase):
    def test_discovers_known_services_from_processes_and_listening_ports(self):
        snapshot = PsutilServiceDiscoveryCollector(FakePsutil()).collect(
            reported_at="2026-06-04T17:35:00+08:00"
        )

        services_by_type = {service.stack_type: service for service in snapshot.services}

        self.assertEqual(snapshot.reported_at, "2026-06-04T17:35:00+08:00")
        self.assertEqual(sorted(services_by_type), ["MYSQL", "NGINX", "NODEJS", "REDIS", "SPRING_BOOT"])
        self.assertEqual(services_by_type["SPRING_BOOT"].service_name, "aegis-backend")
        self.assertEqual(services_by_type["SPRING_BOOT"].ports, [8080])
        self.assertEqual(services_by_type["MYSQL"].ports, [3306])
        self.assertEqual(services_by_type["REDIS"].process_name, "redis-server.exe")
        self.assertEqual(services_by_type["NGINX"].status, "RUNNING")
        self.assertEqual(services_by_type["NODEJS"].command_line, "node.exe server.js")


if __name__ == "__main__":
    unittest.main()
