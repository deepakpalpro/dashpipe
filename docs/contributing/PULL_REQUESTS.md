# Pull Requests

Guidelines for submitting changes to dashpipe-suite.

## Branch naming

Use descriptive prefixes:

- `feature/short-description`
- `fix/issue-number-description`
- `docs/what-you-updated`

Branch from the default branch (`main`).

## Commit messages

Write clear, imperative subject lines:

```
fix(api): reject archived status via PUT
docs: add observability operations guide
feat(ui): show execution step latency in debug panel
```

Conventional commits are encouraged but not enforced.

## PR checklist

Before requesting review:

1. **Scope** — One logical change per PR when possible
2. **Tests** — Run relevant suites ([TESTING.md](TESTING.md))
3. **Docs** — Update docs if you change paths, APIs, or user-visible behavior
4. **Template** — Fill out the [pull request template](../../.github/pull_request_template.md)
5. **License** — You agree contributions are Apache 2.0 ([CONTRIBUTING.md](../../CONTRIBUTING.md))

## Review process

dashpipe-suite uses a **benevolent dictator** model ([GOVERNANCE.md](GOVERNANCE.md)):

- Maintainers review PRs for correctness, tests, and fit with project direction
- One approval from a maintainer is required to merge
- Larger architectural changes may be discussed in an issue first

## What makes a good PR

- Links to an issue (or explains why no issue is needed)
- Includes test evidence (CI green or manual steps)
- Keeps diffs focused — avoid drive-by refactors
- Matches existing code style and conventions

## After merge

Maintainers update [CHANGELOG.md](../../CHANGELOG.md) on release. Patch releases may batch multiple PRs.
