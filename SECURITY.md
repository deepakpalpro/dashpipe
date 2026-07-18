# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |
| < 0.1   | :x:                |

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Report security issues through one of these channels:

1. **GitHub Security Advisories** (preferred): use *Report a vulnerability* on
   the repository Security tab.
2. **Email**: deepak.pal.pro@gmail.com with subject `dashpipe-suite security`.

Include:

- Description of the issue and potential impact
- Steps to reproduce
- Affected component (`platform`, `ci_cd`, `dev-ai-agent`, `tools`, `demo`)
- Version or commit hash if known

## Response Timeline

| Stage | Target |
|-------|--------|
| Initial acknowledgment | 3 business days |
| Triage and severity assessment | 7 business days |
| Fix or mitigation plan | 30 days for high/critical; 90 days for medium/low |
| Coordinated disclosure | After fix is available |

We will credit reporters in the release notes unless you request anonymity.

## Scope

In scope:

- dashpipe-api authentication, tenant isolation, and authorization
- Webhook ingress signature verification
- Secrets handling in deployment manifests and local dev
- MCP server exposure of control-plane APIs

Out of scope (for now):

- Denial-of-service against local dev stacks without a production deployment
- Issues in third-party dependencies without a demonstrable impact on Dashpipe

Thank you for helping keep Dashpipe and its users safe.
