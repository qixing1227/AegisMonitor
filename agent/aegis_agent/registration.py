from aegis_agent.config import AgentConfig
from aegis_agent.host import HostSnapshot
from aegis_agent.http_client import post_json
from aegis_agent.identity import AgentIdentity, AgentStateStore


class AgentRegistrar:
    def __init__(self, config: AgentConfig, state_store: AgentStateStore):
        self._config = config
        self._state_store = state_store

    def register(self, host: HostSnapshot) -> AgentIdentity:
        payload = host.to_registration_payload(
            alias=self._config.host_alias,
            config_summary={
                "hostMetricIntervalSeconds": self._config.host_metric_interval_seconds,
                "heartbeatIntervalSeconds": self._config.heartbeat_interval_seconds,
                "serviceDiscoveryIntervalSeconds": self._config.service_discovery_interval_seconds,
            },
        )
        response = post_json(
            f"{self._config.server_url}/agents/register",
            payload,
            {"X-Agent-Register-Token": self._config.register_token},
        )
        data = response["data"]
        identity = AgentIdentity(
            agent_id=data["agentId"],
            host_id=data["hostId"],
            agent_secret=data["agentSecret"],
        )
        self._state_store.save(identity)
        return identity
