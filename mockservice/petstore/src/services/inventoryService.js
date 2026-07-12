import { getPool, withTransaction } from '../db.js'
import { HttpError } from '../http.js'

export const PRODUCT_CATEGORIES = new Set(['food', 'accessories', 'toys'])
const LOW_STOCK_THRESHOLD = 10

function deriveStatus(quantity) {
  const qty = Number(quantity)
  if (qty <= 0) return 'out_of_stock'
  if (qty < LOW_STOCK_THRESHOLD) return 'low_stock'
  return 'in_stock'
}

function mapItem(row) {
  return {
    id: Number(row.id),
    sku: row.sku,
    name: row.name,
    category: row.category,
    quantity: Number(row.quantity),
    unitPrice: Number(row.unit_price),
    description: row.description ?? undefined,
    status: row.status,
    updatedAt: row.updated_at ? new Date(row.updated_at).toISOString() : undefined,
  }
}

function normalizeItem(input, { requireSku = true } = {}) {
  if (!input || typeof input !== 'object') {
    throw new HttpError(400, 'Invalid inventory item')
  }
  const sku = input.sku != null ? String(input.sku).trim() : ''
  const name = input.name != null ? String(input.name).trim() : ''
  const category = input.category != null ? String(input.category).trim().toLowerCase() : ''
  const quantity = input.quantity != null ? Number(input.quantity) : NaN
  const unitPrice =
    input.unitPrice != null
      ? Number(input.unitPrice)
      : input.unit_price != null
        ? Number(input.unit_price)
        : 0

  if (requireSku && !sku) throw new HttpError(422, "Validation exception: 'sku' is required")
  if (!name) throw new HttpError(422, "Validation exception: 'name' is required")
  if (!PRODUCT_CATEGORIES.has(category)) {
    throw new HttpError(422, "Validation exception: category must be food, accessories, or toys")
  }
  if (!Number.isFinite(quantity) || quantity < 0 || !Number.isInteger(quantity)) {
    throw new HttpError(422, "Validation exception: 'quantity' must be a non-negative integer")
  }
  if (!Number.isFinite(unitPrice) || unitPrice < 0) {
    throw new HttpError(422, "Validation exception: 'unitPrice' must be a non-negative number")
  }

  return {
    sku,
    name,
    category,
    quantity,
    unitPrice,
    description: input.description != null ? String(input.description) : null,
    status: input.status && ['in_stock', 'low_stock', 'out_of_stock'].includes(input.status)
      ? input.status
      : deriveStatus(quantity),
  }
}

export async function listItems({ category, status } = {}) {
  const clauses = []
  const params = []
  if (category) {
    if (!PRODUCT_CATEGORIES.has(category)) {
      throw new HttpError(400, 'Invalid category')
    }
    clauses.push('category = ?')
    params.push(category)
  }
  if (status) {
    clauses.push('status = ?')
    params.push(status)
  }
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : ''
  const [rows] = await getPool().query(
    `SELECT * FROM inventory_items ${where} ORDER BY category, sku`,
    params,
  )
  return rows.map(mapItem)
}

export async function getItemBySku(sku) {
  const [rows] = await getPool().query('SELECT * FROM inventory_items WHERE sku = ?', [sku])
  if (!rows.length) throw new HttpError(404, 'Inventory item not found')
  return mapItem(rows[0])
}

export async function createItem(body) {
  const item = normalizeItem(body)
  try {
    const [result] = await getPool().query(
      `INSERT INTO inventory_items (sku, name, category, quantity, unit_price, description, status)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [item.sku, item.name, item.category, item.quantity, item.unitPrice, item.description, item.status],
    )
    return getItemBySku(item.sku)
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      throw new HttpError(409, `SKU already exists: ${item.sku}`)
    }
    throw err
  }
}

export async function upsertItem(sku, body) {
  const item = normalizeItem({ ...body, sku }, { requireSku: true })
  if (item.sku !== sku) {
    throw new HttpError(400, 'SKU in path and body must match')
  }
  await getPool().query(
    `INSERT INTO inventory_items (sku, name, category, quantity, unit_price, description, status)
     VALUES (?, ?, ?, ?, ?, ?, ?)
     ON DUPLICATE KEY UPDATE
       name = VALUES(name),
       category = VALUES(category),
       quantity = VALUES(quantity),
       unit_price = VALUES(unit_price),
       description = VALUES(description),
       status = VALUES(status)`,
    [item.sku, item.name, item.category, item.quantity, item.unitPrice, item.description, item.status],
  )
  return getItemBySku(sku)
}

export async function deleteItem(sku) {
  const [result] = await getPool().query('DELETE FROM inventory_items WHERE sku = ?', [sku])
  if (result.affectedRows === 0) throw new HttpError(404, 'Inventory item not found')
}

/**
 * Pipeline-friendly bulk upload.
 * mode=upsert (default): insert or update by SKU
 * mode=replace: delete all items in the payload categories, then insert
 */
export async function uploadItems(payload) {
  const itemsInput = Array.isArray(payload) ? payload : payload?.items
  const mode = (Array.isArray(payload) ? 'upsert' : payload?.mode) || 'upsert'

  if (!Array.isArray(itemsInput) || itemsInput.length === 0) {
    throw new HttpError(400, 'Request must include a non-empty items array')
  }
  if (!['upsert', 'replace'].includes(mode)) {
    throw new HttpError(400, "mode must be 'upsert' or 'replace'")
  }

  const items = itemsInput.map((row) => normalizeItem(row))
  const skus = new Set()
  for (const item of items) {
    if (skus.has(item.sku)) {
      throw new HttpError(422, `Duplicate SKU in upload payload: ${item.sku}`)
    }
    skus.add(item.sku)
  }

  const result = await withTransaction(async (conn) => {
    let created = 0
    let updated = 0
    let deleted = 0

    if (mode === 'replace') {
      const categories = [...new Set(items.map((i) => i.category))]
      const [del] = await conn.query(
        `DELETE FROM inventory_items WHERE category IN (${categories.map(() => '?').join(',')})`,
        categories,
      )
      deleted = del.affectedRows
    }

    for (const item of items) {
      if (mode === 'replace') {
        await conn.query(
          `INSERT INTO inventory_items (sku, name, category, quantity, unit_price, description, status)
           VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [item.sku, item.name, item.category, item.quantity, item.unitPrice, item.description, item.status],
        )
        created += 1
        continue
      }

      const [existing] = await conn.query('SELECT id FROM inventory_items WHERE sku = ?', [item.sku])
      if (existing.length) {
        await conn.query(
          `UPDATE inventory_items
           SET name = ?, category = ?, quantity = ?, unit_price = ?, description = ?, status = ?
           WHERE sku = ?`,
          [item.name, item.category, item.quantity, item.unitPrice, item.description, item.status, item.sku],
        )
        updated += 1
      } else {
        await conn.query(
          `INSERT INTO inventory_items (sku, name, category, quantity, unit_price, description, status)
           VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [item.sku, item.name, item.category, item.quantity, item.unitPrice, item.description, item.status],
        )
        created += 1
      }
    }

    return { mode, received: items.length, created, updated, deleted }
  })

  const summary = await getProductSummary()
  return { ...result, summary }
}

export async function getProductSummary() {
  const [rows] = await getPool().query(
    `SELECT category, COUNT(*) AS items, COALESCE(SUM(quantity), 0) AS quantity
     FROM inventory_items
     GROUP BY category`,
  )
  const byCategory = { food: { items: 0, quantity: 0 }, accessories: { items: 0, quantity: 0 }, toys: { items: 0, quantity: 0 } }
  for (const row of rows) {
    byCategory[row.category] = {
      items: Number(row.items),
      quantity: Number(row.quantity),
    }
  }
  return byCategory
}
