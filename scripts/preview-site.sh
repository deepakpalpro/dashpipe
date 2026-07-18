#!/usr/bin/env bash
# Build docs with Jekyll (Docker) and serve the full site locally.
#
# Usage: ./scripts/preview-site.sh [port]
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${1:-8088}"
SITE="$ROOT/.site-preview"
DOCS_SRC="$SITE/docs-src"

rm -rf "$SITE"
mkdir -p "$DOCS_SRC" "$SITE/docs"

echo "==> Staging docs (add Jekyll front matter where missing)..."
rsync -a --exclude '_site' "$ROOT/docs/" "$DOCS_SRC/"
python3 - "$DOCS_SRC" << 'PY'
import sys
from pathlib import Path

root = Path(sys.argv[1])
for path in sorted(root.rglob("*.md")):
    if any(part.startswith("_") for part in path.relative_to(root).parts):
        continue
    text = path.read_text(encoding="utf-8")
    if text.startswith("---"):
        continue
    title = text.splitlines()[0].lstrip("# ").strip() if text else path.stem
    path.write_text(f"---\nlayout: default\ntitle: {title}\n---\n\n{text}", encoding="utf-8")
PY

echo "==> Building docs (Jekyll)..."
docker run --rm \
  -v "$DOCS_SRC:/srv/jekyll" \
  -v "$SITE/docs:/srv/jekyll/_site" \
  jekyll/jekyll:4 \
  jekyll build

echo "==> Adding landing page and assets..."
cp "$ROOT/index.html" "$SITE/"
cp -r "$ROOT/website" "$SITE/website"

echo "==> Serving http://127.0.0.1:${PORT}/"
echo "    Docs: http://127.0.0.1:${PORT}/docs/README.html"
cd "$SITE"
exec python3 -m http.server "$PORT"
