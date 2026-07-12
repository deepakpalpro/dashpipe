# Petstore Inventory mock

Standalone GET-only inventory catalog for Dashflow **REST source** demos (`plet-rest-source`).
In-memory store seeded from [`data/inventory-seed.json`](data/inventory-seed.json) (same SKUs as `petstore/samples/inventory.csv`).

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Liveness |
| `GET` | `/api/v3/inventory/items` | All items (`?category=food&status=in_stock`) |
| `GET` | `/api/v3/inventory/items/{sku}` | One SKU |
| `GET` | `/api/v3/inventory/summary` | Counts by category / status |

Default port: **4011** (full Petstore mock remains on **4010**).

## Run locally

```bash
cd mockservice/petstore-inventory
npm install
npm start
curl -s http://localhost:4011/api/v3/inventory/items | python3 -m json.tool | head
curl -s http://localhost:4011/api/v3/inventory/summary | python3 -m json.tool
```

## Docker Compose

Included under the `petstore` profile as `petstore-inventory`:

```bash
docker compose --profile petstore up -d petstore-inventory
# or via localdev (starts with petstore)
./scripts/localdev.sh start
```

## REST source connector

Seed connector `conn-plet-rest-source` points at:

`http://host.docker.internal:4011/api/v3`
