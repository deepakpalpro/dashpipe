# `plet-file-destination`

Category: **Destination**. Connector: `storage`. Behavior: `dest_file`.

Writes pipeline `payload` / `records` to a **local or mounted path**.

### Config

| Source | Key | Purpose |
|--------|-----|---------|
| Connector or execution | `basePath` / `mountPath` / `path` | Absolute mount root (required) |
| Execution | `objectKey` | Relative file under `basePath` (required) |
| Execution | `format` | `json` (default) or `ndjson` / `jsonl` |
| Execution | `append` | `true` to append instead of overwrite |

### Build

```bash
docker build -f pipelets/destination/storage/plet-file-destination/Dockerfile \
  -t dashpipe/plet-file-destination:local pipelets
```

For K8s Jobs, mount a volume at `basePath` (e.g. hostPath `/tmp/dashpipe-out` → `/data/out`).
