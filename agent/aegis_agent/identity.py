import json
from dataclasses import asdict, dataclass
from pathlib import Path


@dataclass(frozen=True)
class AgentIdentity:
    agent_id: str
    host_id: str
    agent_secret: str


class AgentStateStore:
    def __init__(self, path: str | Path):
        self._path = Path(path)

    def save(self, identity: AgentIdentity) -> None:
        self._path.parent.mkdir(parents=True, exist_ok=True)
        self._path.write_text(
            json.dumps(asdict(identity), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    def load(self) -> AgentIdentity | None:
        if not self._path.exists():
            return None
        data = json.loads(self._path.read_text(encoding="utf-8"))
        return AgentIdentity(
            agent_id=data["agent_id"],
            host_id=data["host_id"],
            agent_secret=data["agent_secret"],
        )
