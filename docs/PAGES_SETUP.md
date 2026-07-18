# GitHub Pages setup

| File | Role |
|------|------|
| [`../index.html`](../index.html) | Landing page at site root (`/`) |
| [`website/`](../website/) | CSS, JS, and `CNAME` for custom domain |
| [`README.md`](README.md) | Documentation hub (`/docs/README.html`) |

## Publish source

**Recommended:** **Settings → Pages → Source → GitHub Actions**

Push to `main` runs [`.github/workflows/pages.yml`](../.github/workflows/pages.yml):

1. Jekyll builds `docs/` into a temp folder `docs-build/`
2. The workflow assembles `_site/` (runner-owned): `docs-build/` → `_site/docs/`, plus root `index.html` and `website/`

**Alternative:** **Deploy from branch → `/ (root)`**

`index.html` at repo root is served at `/`. Asset paths use `website/css/` and `website/js/`.

## Custom domain

`website/CNAME` contains `dashpipe.io`. On Actions deploy it is copied to the site root.

DNS: apex **A records** to GitHub Pages (`185.199.108.153`, …) or **CNAME** `www` → `deepakpalpro.github.io`.

Then **Settings → Pages → Custom domain** → `dashpipe.io` → **Enforce HTTPS**.

## Local preview

Doc links on the landing page point to `/docs/*.html` (Jekyll output). A plain `python3 -m http.server` will **not** build those HTML files.

```bash
./scripts/preview-site.sh
# http://127.0.0.1:8088/  — landing page with working doc links
```

Requires Docker (for Jekyll). On GitHub Pages, the same paths work after the Actions deploy.
