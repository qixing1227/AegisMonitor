import json
from urllib import request


def post_json(url: str, payload: dict, headers: dict[str, str] | None = None) -> dict:
    encoded = json.dumps(payload).encode("utf-8")
    req = request.Request(
        url,
        data=encoded,
        headers={
            **(headers or {}),
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        method="POST",
    )
    with request.urlopen(req, timeout=10) as response:
        body = response.read().decode("utf-8")
    if not body:
        return {}
    return json.loads(body)

