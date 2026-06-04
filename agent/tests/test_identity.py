import tempfile
import unittest
from pathlib import Path

from aegis_agent.identity import AgentIdentity, AgentStateStore


class AgentIdentityTest(unittest.TestCase):
    def test_persists_registered_agent_identity(self):
        with tempfile.TemporaryDirectory() as tmp:
            state_file = Path(tmp) / ".agent-state.json"
            store = AgentStateStore(state_file)
            identity = AgentIdentity(
                agent_id="agt_001",
                host_id="host_001",
                agent_secret="secret-value",
            )

            store.save(identity)

            self.assertEqual(store.load(), identity)


if __name__ == "__main__":
    unittest.main()
