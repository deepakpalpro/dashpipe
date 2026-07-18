# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Open-source documentation hub under `docs/`
- Apache 2.0 license, contributor guidelines, and security policy
- GitHub issue and pull request templates

### Changed

- **BREAKING**: Project renamed from Dashflow to **Dashpipe**
- All monorepo paths (`dashpipe-platform`, `dashpipe-ci_cd`, etc.), Java packages (`com.dashpipe`), Docker image tags (`dashpipe/*`), K8s namespace/labels (`dashpipe`, `dashpipe.io/*`), and env vars (`DASHPIPE_*`) updated
- Existing local Docker images tagged `dashflow/*` must be rebuilt as `dashpipe/*`
- MCP Cursor config must use `DASHPIPE_*` env vars instead of `DASHFLOW_*`

## [0.1.0] - 2026-07-18

### Added

- **dashpipe-suite** monorepo with five components:
  - `dashpipe-platform` — API, UI, SPI, broker, pipelets
  - `dashpipe-ci_cd` — Docker/K8s/Azure deploy and localdev scripts
  - `dashpipe-dev-ai-agent` — LangGraph pipeline guide, pipelet developer, MCP server
  - `dashpipe-tools` — Prometheus, Grafana, ELK
  - `dashpipe-demo` — Petstore mocks and LocalStack
- Root `docker-compose.yml` with Compose `include` for platform, demo, and tools
- GitHub Actions CI for platform, AI agent, MCP, and compose smoke tests

### Changed

- Restructured from standalone `dashflow` and `ai-dataflow-developer` repos into a single monorepo

[Unreleased]: https://github.com/your-org/dashpipe-suite/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/your-org/dashpipe-suite/releases/tag/v0.1.0
