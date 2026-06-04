from aegis_agent.config import AgentConfig
from aegis_agent.http_client import post_json
from aegis_agent.identity import AgentIdentity


class HeartbeatReporter:
    def __init__(self, config: AgentConfig):
        self._config = config

    def report(self, identity: AgentIdentity, status: str, reported_at: str) -> None:
        payload = {
            "agentId": identity.agent_id,
            "hostId": identity.host_id,
            "status": status,
            "reportedAt": reported_at,
        }
        post_json(
            f"{self._config.server_url}/agents/heartbeat",
            payload,
            headers={
                "X-Agent-Id": identity.agent_id,
                "X-Agent-Secret": identity.agent_secret,
            },
        )
