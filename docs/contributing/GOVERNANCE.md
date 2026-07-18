# Governance

How decisions are made in the dashpipe-suite open-source project.

## Model

dashpipe-suite follows a **benevolent dictator** governance model:

- **Maintainers** review and merge contributions
- The **lead maintainer** has final authority on direction, releases, and disputed technical decisions
- The community participates through issues, discussions, and pull requests

This keeps the project agile while remaining open to contributions.

## Roles

| Role | Responsibilities |
|------|------------------|
| **Contributor** | Opens issues and PRs, follows Code of Conduct |
| **Maintainer** | Reviews PRs, triages issues, cuts releases |
| **Lead maintainer** | Sets roadmap priorities, resolves disputes, appoints maintainers |

## Decision process

| Decision type | Process |
|---------------|---------|
| Bug fixes, docs, tests | PR review by any maintainer |
| New pipelets or connectors | PR review; should align with SPI and catalog conventions |
| API or schema changes | Issue discussion recommended; maintainer approval required |
| Breaking changes | Issue with migration plan; explicit approval from lead maintainer |
| Roadmap priorities | Maintainers + community input via issues; lead maintainer decides |

## Becoming a maintainer

Maintainer status is granted to contributors who:

- Submit multiple high-quality, merged PRs over time
- Demonstrate understanding of architecture and testing standards
- Help review others' work constructively
- Uphold the [Code of Conduct](../../CODE_OF_CONDUCT.md)

There is no fixed quota — recognition is based on sustained, trusted contribution.

## RFCs (optional)

For large features (new broker adapter, multi-region support, major UI rewrite), open a GitHub issue labeled `rfc` with:

- Problem statement
- Proposed design
- Alternatives considered
- Impact on existing deployments

Wait for maintainer feedback before investing in a large PR.

## Releases

- Version tags follow [Semantic Versioning](https://semver.org/)
- [CHANGELOG.md](../../CHANGELOG.md) records user-visible changes
- Security fixes may be released out of band — see [SECURITY.md](../../SECURITY.md)

## Code of conduct enforcement

See [CODE_OF_CONDUCT.md](../../CODE_OF_CONDUCT.md). Maintainers may restrict participation for violations.
