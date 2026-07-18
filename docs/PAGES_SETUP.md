# GitHub Pages setup

| File | Role |
|------|------|
| [`../index.html`](../index.html) | Landing page at site root (`/`) |
| [`website/`](../website/) | CSS, JS, and `CNAME` for custom domain |
| [`README.md`](README.md) | Documentation hub (`/docs/README.html`) |

## Publish source

**Recommended:** **Settings → Pages → Source → GitHub Actions**

Push to `main` runs [`.github/workflows/pages.yml`](../.github/workflows/pages.yml):

1. Jekyll builds `docs/` into `/docs/` on the site
2. Root `index.html` and `website/` are added; `website/CNAME` is copied to `/CNAME`

**Alternative:** **Deploy from branch → `/ (root)`**

`index.html` at repo root is served at `/`. Asset paths use `website/css/` and `website/js/`.

## Custom domain

`website/CNAME` contains `dashpipe.io`. On Actions deploy it is copied to the site root.

DNS: apex **A records** to GitHub Pages (`185.199.108.153`, …) or **CNAME** `www` → `deepakpalpro.github.io`.

Then **Settings → Pages → Custom domain** → `dashpipe.io` → **Enforce HTTPS**.

## Local preview

```bash
python3 -m http.server 8088
# http://localhost:8088/index.html
```
