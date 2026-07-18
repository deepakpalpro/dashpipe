# Petstore API (MySQL)

End-to-end Swagger Petstore service implementing [OpenAPI 3.0 Petstore](https://petstore3.swagger.io/api/v3/openapi.json) with **MySQL** persistence.

## Prerequisites

- Node.js >= 18.20.1
- Docker Compose MySQL from the repo root (`mysql:8.4` on port 3306)

## Setup

```bash
# From repo root — start shared MySQL
docker compose up -d mysql

# Install + create schema/seed
cd mockservice/petstore
cp .env.example .env   # optional; defaults match Compose credentials
npm install
npm run db:seed
```

`db:seed` creates the `petstore` database (via root), applies schema, and loads sample pets/users/orders.

> If MySQL was already initialized before the `docker/mysql-init` mount was added, `npm run db:seed` still creates the DB and grants for the `pipeline` user.

## Run

```bash
npm start
```

Listens on **http://localhost:4010** with OpenAPI base path **`/api/v3`**.

Or via Compose (same MySQL):

```bash
docker compose --profile petstore up -d --build petstore
```

```bash
curl http://localhost:4010/health

curl http://localhost:4010/api/v3/pet/1
curl 'http://localhost:4010/api/v3/pet/findByStatus?status=available'
curl -H 'api_key: special-key' http://localhost:4010/api/v3/store/inventory

curl -X POST http://localhost:4010/api/v3/pet \
  -H 'Content-Type: application/json' \
  -d '{"name":"rex","photoUrls":["https://example.com/rex.jpg"],"status":"available"}'

curl -X POST http://localhost:4010/api/v3/store/order \
  -H 'Content-Type: application/json' \
  -d '{"petId":1,"quantity":1,"status":"placed"}'

curl 'http://localhost:4010/api/v3/user/login?username=user1&password=12345'
```

## Orders for invoice pipelines

Seeded orders include customer, unit price, and invoice numbers so a pipeline can import them.

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/v3/store/orders` | List all orders (invoice import source) |
| `GET` | `/api/v3/store/orders?status=delivered&complete=true` | Invoice candidates only |
| `GET` | `/api/v3/store/order/{id}` | Single order |
| `POST` | `/api/v3/store/order` | Place a new order |

Sample CSV (for S3 → invoice pipeline): [`samples/orders.csv`](./samples/orders.csv)

```bash
# All orders
curl http://localhost:4010/api/v3/store/orders

# Ready-to-invoice (delivered + complete)
curl 'http://localhost:4010/api/v3/store/orders?status=delivered&complete=true'

# Place an invoice-ready order
curl -X POST http://localhost:4010/api/v3/store/order \
  -H 'Content-Type: application/json' \
  -d '{
    "petId": 1,
    "customerUsername": "user1",
    "quantity": 1,
    "unitPrice": 99.99,
    "currency": "USD",
    "status": "delivered",
    "complete": true,
    "notes": "Pipeline demo order"
  }'
```

Example list item shape:

```json
{
  "orderId": 3,
  "invoiceNumber": "INV-0003",
  "customerUsername": "user1",
  "customerEmail": "john@email.com",
  "customerName": "John James",
  "petId": 2,
  "petName": "kitty",
  "quantity": 1,
  "unitPrice": 79,
  "lineTotal": 79,
  "currency": "USD",
  "status": "delivered",
  "complete": true
}
```

## Product inventory (food / accessories / toys)

Pipeline-friendly catalog for non-pet stock. Categories: `food`, `accessories`, `toys`.

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v3/inventory/upload` | **Bulk upsert** from a data pipeline |
| `GET` | `/api/v3/inventory/items` | List items (`?category=food&status=in_stock`) |
| `GET` | `/api/v3/inventory/items/{sku}` | Get one SKU |
| `POST` | `/api/v3/inventory/items` | Create one item |
| `PUT` | `/api/v3/inventory/items/{sku}` | Upsert one SKU |
| `DELETE` | `/api/v3/inventory/items/{sku}` | Delete one SKU |
| `GET` | `/api/v3/inventory/summary` | Counts by category |
| `GET` | `/api/v3/store/inventory` | Pets + product summary |

### Pipeline bulk upload example

```bash
curl -X POST http://localhost:4010/api/v3/inventory/upload \
  -H 'Content-Type: application/json' \
  -d '{
    "mode": "upsert",
    "items": [
      {
        "sku": "FOOD-100",
        "name": "Puppy Formula 2kg",
        "category": "food",
        "quantity": 200,
        "unitPrice": 24.99,
        "description": "Grain-free puppy food"
      },
      {
        "sku": "TOY-050",
        "name": "Squeaky Bone",
        "category": "toys",
        "quantity": 75,
        "unitPrice": 6.50
      },
      {
        "sku": "ACC-020",
        "name": "Nylon Collar M",
        "category": "accessories",
        "quantity": 40,
        "unitPrice": 12.00
      }
    ]
  }'
```

End-to-end from LocalStack S3 CSV through Docker/K8s pipelets:

```bash
./scripts/inventory-pipeline-e2e.sh          # batch container
./scripts/inventory-pipeline-e2e.sh --stages # one container per pipelet
./scripts/inventory-pipeline-e2e.sh --k8s    # Job in current cluster
```

See [`pipelets/inventory/README.md`](../../pipelets/inventory/README.md).

`mode` options:
- `upsert` (default) — insert or update by `sku`
- `replace` — delete existing rows in the payload categories, then insert

### Single item upsert

```bash
curl -X PUT http://localhost:4010/api/v3/inventory/items/FOOD-100 \
  -H 'Content-Type: application/json' \
  -d '{
    "sku": "FOOD-100",
    "name": "Puppy Formula 2kg",
    "category": "food",
    "quantity": 180,
    "unitPrice": 24.99
  }'
```

## Scripts

| Script | Description |
|--------|-------------|
| `npm start` | Run the MySQL-backed API on port 4010 |
| `npm run dev` | Same with `--watch` |
| `npm run db:migrate` | Create DB + apply schema |
| `npm run db:seed` | Schema + sample data |
| `npm run mock` | Optional Prism mock on port **4011** (no DB) |

## Database

| Setting | Default |
|---------|---------|
| Host | `127.0.0.1:3306` |
| Database | `petstore` |
| User / password | `pipeline` / `pipeline` |
| Root (migrate only) | `root` / `root` |

Tables: `categories`, `tags`, `pets`, `pet_photos`, `pet_tags`, `orders`, `users`, `inventory_items`.

## Layout

```
mockservice/petstore/
  openapi.json          # Petstore OpenAPI 3.0
  sql/                  # schema + seed (+ inventory)
  src/
    index.js            # server entry
    app.js              # Express app
    db.js / config.js
    migrate.js
    routes/             # pet, store, user, inventory
    services/           # MySQL business logic
```
