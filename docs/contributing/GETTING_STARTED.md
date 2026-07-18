# Getting Started as a Contributor

Thank you for contributing to Dashpipe!

## 1. Fork and clone

```bash
git clone https://github.com/YOUR_USER/dashpipe-suite.git
cd dashpipe-suite
git remote add upstream https://github.com/your-org/dashpipe-suite.git
```

## 2. Prerequisites

Install the tools for the component you plan to work on:

| Component | Required tools |
|-----------|----------------|
| Platform (Java/UI) | Java 21, Maven (wrapper included), Node 20+, Docker |
| CI/CD scripts | Docker, bash, optional `kubectl`/`az` |
| AI agent / MCP | Python 3.12+, Docker |
| Tools / Demo | Docker |

## 3. Start the dev stack

```bash
./dashpipe-ci_cd/scripts/localdev.sh start --with-metrics
```

Confirm:

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://127.0.0.1:5173
```

## 4. Create a branch

```bash
git checkout -b feature/short-description
```

Branch from `main` (or the current default branch).

## 5. Make your change

Follow [DEVELOPMENT.md](DEVELOPMENT.md) for component-specific commands.

## 6. Run tests

See [TESTING.md](TESTING.md). At minimum, run tests for the component you touched.

## 7. Open a pull request

Follow [PULL_REQUESTS.md](PULL_REQUESTS.md). Fill out the PR template completely.

## IDE tips

- **Java:** IntelliJ or VS Code with Extension Pack for Java; import `dashpipe-platform` as Maven project
- **UI:** Open `dashpipe-platform/dashpipe-ui`; Vite dev server starts via localdev or `npx vite`
- **Python:** Use a venv per package (`dashpipe-dev-ai-agent/api`, `dashpipe-mcp`)

## Communication

- **Bugs:** GitHub issue with bug report template
- **Features:** Feature request issue before large PRs
- **Security:** See [SECURITY.md](../../SECURITY.md) — no public issues

Read the [Code of Conduct](../../CODE_OF_CONDUCT.md) before participating.
