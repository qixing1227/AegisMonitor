from dataclasses import asdict, dataclass


@dataclass(frozen=True)
class HostSnapshot:
    hostname: str
    ip_address: str
    os_name: str
    os_version: str
    cpu_cores: int
    memory_total_bytes: int
    boot_time: str
    agent_version: str

    def to_registration_payload(self, alias: str, config_summary: dict[str, int]) -> dict:
        payload = asdict(self)
        return {
            "hostname": payload["hostname"],
            "alias": alias,
            "ipAddress": payload["ip_address"],
            "osName": payload["os_name"],
            "osVersion": payload["os_version"],
            "cpuCores": payload["cpu_cores"],
            "memoryTotalBytes": payload["memory_total_bytes"],
            "bootTime": payload["boot_time"],
            "agentVersion": payload["agent_version"],
            "configSummary": config_summary,
        }

