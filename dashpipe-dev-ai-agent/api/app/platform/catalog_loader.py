"""Load live pipelet catalog from platform when reachable."""

from __future__ import annotations

from app.platform.settings import PlatformSettings
from app.schemas import CatalogPipeletSummary
from dashpipe_mcp.platform_ops import PlatformOps


def load_live_catalog(settings: PlatformSettings | None = None, limit: int = 80) -> list[CatalogPipeletSummary]:
    cfg = settings or PlatformSettings.from_env()
    ops = PlatformOps(cfg.to_mcp_settings())
    try:
        ops.dashpipe_health()
        pipelets = ops.list_pipelets(active_only=True, limit=limit)
    except Exception:
        return []
    finally:
        ops.close()

    summaries: list[CatalogPipeletSummary] = []
    for item in pipelets:
        pid = item.get("id") or item.get("pipelet_id")
        if not pid:
            continue
        exec_keys = item.get("required_execution_keys") or item.get("requiredExecutionKeys") or []
        summaries.append(
            CatalogPipeletSummary(
                id=str(pid),
                name=item.get("name"),
                category=item.get("category"),
                description=item.get("description"),
                required_execution_keys=[str(k) for k in exec_keys],
            )
        )
    return summaries
