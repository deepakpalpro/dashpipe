import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const seedPath = path.join(__dirname, '..', 'data', 'inventory-seed.json')

function deriveStatus(quantity) {
  const q = Number(quantity) || 0
  if (q <= 0) return 'out_of_stock'
  if (q < 10) return 'low_stock'
  return 'in_stock'
}

function loadSeed() {
  const raw = JSON.parse(readFileSync(seedPath, 'utf8'))
  if (!Array.isArray(raw)) {
    throw new Error('inventory-seed.json must be a JSON array')
  }
  return raw.map((row) => {
    const quantity = Number(row.quantity) || 0
    return {
      sku: String(row.sku),
      name: String(row.name || ''),
      category: String(row.category || 'misc'),
      quantity,
      unit_price: Number(row.unit_price) || 0,
      description: String(row.description || ''),
      status: row.status || deriveStatus(quantity),
    }
  })
}

/** In-memory inventory store, seeded at process start. */
export class InventoryStore {
  constructor(items = loadSeed()) {
    this._items = new Map(items.map((item) => [item.sku, { ...item }]))
  }

  list({ category, status } = {}) {
    let rows = [...this._items.values()]
    if (category) {
      rows = rows.filter((r) => r.category === category)
    }
    if (status) {
      rows = rows.filter((r) => r.status === status)
    }
    return rows.sort((a, b) => {
      const c = a.category.localeCompare(b.category)
      return c !== 0 ? c : a.sku.localeCompare(b.sku)
    })
  }

  get(sku) {
    return this._items.get(sku) || null
  }

  summary() {
    const byCategory = {}
    let totalQty = 0
    let skus = 0
    for (const item of this._items.values()) {
      skus += 1
      totalQty += item.quantity
      byCategory[item.category] = (byCategory[item.category] || 0) + 1
    }
    return {
      skuCount: skus,
      totalQuantity: totalQty,
      byCategory,
      byStatus: this._statusCounts(),
    }
  }

  _statusCounts() {
    const counts = {}
    for (const item of this._items.values()) {
      counts[item.status] = (counts[item.status] || 0) + 1
    }
    return counts
  }
}

export function createStore() {
  return new InventoryStore()
}
