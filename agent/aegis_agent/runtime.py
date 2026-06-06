from collections.abc import Callable
import time

from aegis_agent.config import AgentConfig
from aegis_agent.heartbeat import HeartbeatReporter
from aegis_agent.host import HostSnapshot
from aegis_agent.identity import AgentIdentity, AgentStateStore
from aegis_agent.metrics import HostMetricReporter, HostMetricSnapshot
from aegis_agent.registration import AgentRegistrar
from aegis_agent.service_discovery import ServiceDiscoveryReporter


class AgentRuntime:
    def __init__(
        self,
        config: AgentConfig,
        state_store: AgentStateStore,
        host_snapshot_provider: Callable[[], HostSnapshot],
        metric_collector,
        service_discovery_collector=None,
    ):
        self._config = config
        self._state_store = state_store
        self._host_snapshot_provider = host_snapshot_provider
        self._metric_collector = metric_collector
        self._service_discovery_collector = service_discovery_collector

    def run_once(self, reported_at: str) -> AgentIdentity:
        identity = self._state_store.load()
        if identity is None:
            identity = AgentRegistrar(self._config, self._state_store).register(
                self._host_snapshot_provider()
            )

        HeartbeatReporter(self._config).report(
            identity,
            status="ONLINE",
            reported_at=reported_at,
        )
        snapshot: HostMetricSnapshot = self._metric_collector.collect(reported_at)
        HostMetricReporter(self._config).report(identity, snapshot)

        if self._service_discovery_collector is not None:
            service_snapshot = self._service_discovery_collector.collect(reported_at)
            ServiceDiscoveryReporter(self._config).report(identity, service_snapshot)

        return identity

    def run_forever(
        self,
        reported_at_provider: Callable[[], str],
        sleeper: Callable[[int], None] = time.sleep,
        max_iterations: int | None = None,
    ) -> AgentIdentity | None:
        identity = None
        iterations = 0

        while max_iterations is None or iterations < max_iterations:
            identity = self.run_once(reported_at_provider())
            iterations += 1

            if max_iterations is not None and iterations >= max_iterations:
                break

            sleeper(self._config.host_metric_interval_seconds)

        return identity
