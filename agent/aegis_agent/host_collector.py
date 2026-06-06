import ctypes
import os
import platform
import socket
from datetime import datetime, timedelta, timezone

from aegis_agent.host import HostSnapshot


def collect_host_snapshot(agent_version: str) -> HostSnapshot:
    return HostSnapshot(
        hostname=socket.gethostname(),
        ip_address=_detect_ip_address(),
        os_name=platform.system() or "Unknown",
        os_version=platform.version() or platform.release() or "Unknown",
        cpu_cores=os.cpu_count() or 1,
        memory_total_bytes=_memory_total_bytes(),
        boot_time=_boot_time_iso(),
        agent_version=agent_version,
    )


def _detect_ip_address() -> str:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.connect(("8.8.8.8", 80))
            return sock.getsockname()[0]
    except OSError:
        return "127.0.0.1"


def _memory_total_bytes() -> int:
    if platform.system().lower() == "windows":
        return _windows_memory_total_bytes()
    return _posix_memory_total_bytes()


def _windows_memory_total_bytes() -> int:
    class MemoryStatusEx(ctypes.Structure):
        _fields_ = [
            ("dwLength", ctypes.c_ulong),
            ("dwMemoryLoad", ctypes.c_ulong),
            ("ullTotalPhys", ctypes.c_ulonglong),
            ("ullAvailPhys", ctypes.c_ulonglong),
            ("ullTotalPageFile", ctypes.c_ulonglong),
            ("ullAvailPageFile", ctypes.c_ulonglong),
            ("ullTotalVirtual", ctypes.c_ulonglong),
            ("ullAvailVirtual", ctypes.c_ulonglong),
            ("ullAvailExtendedVirtual", ctypes.c_ulonglong),
        ]

    status = MemoryStatusEx()
    status.dwLength = ctypes.sizeof(MemoryStatusEx)
    if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(status)):
        raise OSError("GlobalMemoryStatusEx failed")
    return int(status.ullTotalPhys)


def _posix_memory_total_bytes() -> int:
    if hasattr(os, "sysconf"):
        try:
            page_size = os.sysconf("SC_PAGE_SIZE")
            pages = os.sysconf("SC_PHYS_PAGES")
            return int(page_size * pages)
        except (OSError, ValueError):
            pass
    return 1


def _boot_time_iso() -> str:
    now = datetime.now(timezone.utc)
    if platform.system().lower() == "windows":
        millis_since_boot = ctypes.windll.kernel32.GetTickCount64()
        boot_time = now - timedelta(milliseconds=millis_since_boot)
    else:
        boot_time = now
    return boot_time.astimezone().isoformat(timespec="seconds")
