import unittest

from aegis_agent.host_collector import collect_host_snapshot


class HostCollectorTest(unittest.TestCase):
    def test_collects_host_snapshot_for_registration(self):
        snapshot = collect_host_snapshot(agent_version="0.1.0")

        self.assertTrue(snapshot.hostname)
        self.assertTrue(snapshot.ip_address)
        self.assertTrue(snapshot.os_name)
        self.assertTrue(snapshot.os_version)
        self.assertGreaterEqual(snapshot.cpu_cores, 1)
        self.assertGreater(snapshot.memory_total_bytes, 0)
        self.assertIn("T", snapshot.boot_time)
        self.assertEqual(snapshot.agent_version, "0.1.0")


if __name__ == "__main__":
    unittest.main()
