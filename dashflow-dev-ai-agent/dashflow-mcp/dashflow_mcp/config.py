from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="DASHFLOW_", env_file=".env", extra="ignore")

    api_url: str = "http://localhost:8080"
    tenant_id: str = "T001"
    pipelets_catalog: Path | None = None
    request_timeout_s: float = 60.0

    def resolve_pipelets_catalog(self, repo_root: Path | None = None) -> Path:
        if self.pipelets_catalog is not None:
            return self.pipelets_catalog.expanduser().resolve()
        root = repo_root or Path(__file__).resolve().parents[3]
        return root / "dashflow-platform" / "dashflow-ui" / "src" / "fixtures" / "pipelets.json"


settings = Settings()
