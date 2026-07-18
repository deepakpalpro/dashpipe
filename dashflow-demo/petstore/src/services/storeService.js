import { getPool } from '../db.js'
import { HttpError } from '../http.js'
import { getProductSummary } from './inventoryService.js'

const ORDER_STATUSES = new Set(['placed', 'approved', 'delivered'])

function invoiceNumber(orderId) {
  return `INV-${String(orderId).padStart(4, '0')}`
}

function mapOrder(row) {
  const quantity = Number(row.quantity)
  const unitPrice = Number(row.unit_price ?? 0)
  return {
    id: Number(row.id),
    orderId: Number(row.id),
    invoiceNumber: invoiceNumber(row.id),
    petId: Number(row.pet_id),
    petName: row.pet_name ?? undefined,
    customerUsername: row.customer_username ?? undefined,
    customerEmail: row.customer_email ?? undefined,
    customerName:
      row.first_name || row.last_name
        ? [row.first_name, row.last_name].filter(Boolean).join(' ')
        : undefined,
    quantity,
    unitPrice,
    lineTotal: Number((quantity * unitPrice).toFixed(2)),
    currency: row.currency ?? 'USD',
    shipDate: row.ship_date ? new Date(row.ship_date).toISOString() : undefined,
    status: row.status,
    complete: Boolean(row.complete),
    notes: row.notes ?? undefined,
    createdAt: row.created_at ? new Date(row.created_at).toISOString() : undefined,
  }
}

const ORDER_SELECT = `
  SELECT o.*,
         p.name AS pet_name,
         u.email AS customer_email,
         u.first_name,
         u.last_name
  FROM orders o
  LEFT JOIN pets p ON p.id = o.pet_id
  LEFT JOIN users u ON u.username = o.customer_username
`

export async function getInventory() {
  const [petRows] = await getPool().query(
    `SELECT status, COUNT(*) AS qty FROM pets GROUP BY status`,
  )
  const pets = {}
  for (const row of petRows) {
    pets[row.status] = Number(row.qty)
  }

  const products = await getProductSummary()
  return { pets, products }
}

export async function listOrders({ status, complete } = {}) {
  const clauses = []
  const params = []
  if (status) {
    if (!ORDER_STATUSES.has(status)) throw new HttpError(400, 'Invalid status value')
    clauses.push('o.status = ?')
    params.push(status)
  }
  if (complete !== undefined) {
    clauses.push('o.complete = ?')
    params.push(complete ? 1 : 0)
  }
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : ''
  const [rows] = await getPool().query(
    `${ORDER_SELECT} ${where} ORDER BY o.id`,
    params,
  )
  return rows.map(mapOrder)
}

export async function placeOrder(body) {
  if (body == null || typeof body !== 'object') {
    throw new HttpError(400, 'Invalid input')
  }
  if (body.petId == null || body.quantity == null) {
    throw new HttpError(422, 'Validation exception: petId and quantity are required')
  }
  if (body.status && !ORDER_STATUSES.has(body.status)) {
    throw new HttpError(400, 'Invalid input')
  }

  const [pets] = await getPool().query('SELECT id FROM pets WHERE id = ?', [body.petId])
  if (!pets.length) throw new HttpError(422, 'Validation exception: pet not found')

  const customerUsername = body.customerUsername ?? body.username ?? null
  if (customerUsername) {
    const [users] = await getPool().query('SELECT username FROM users WHERE username = ?', [
      customerUsername,
    ])
    if (!users.length) throw new HttpError(422, 'Validation exception: customer not found')
  }

  const unitPrice =
    body.unitPrice != null
      ? Number(body.unitPrice)
      : body.unit_price != null
        ? Number(body.unit_price)
        : 0
  if (!Number.isFinite(unitPrice) || unitPrice < 0) {
    throw new HttpError(422, 'Validation exception: unitPrice must be a non-negative number')
  }

  const [result] = await getPool().query(
    `INSERT INTO orders
       (pet_id, customer_username, quantity, unit_price, currency, ship_date, status, complete, notes)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      body.petId,
      customerUsername,
      body.quantity,
      unitPrice,
      body.currency ?? 'USD',
      body.shipDate ? new Date(body.shipDate) : null,
      body.status ?? 'placed',
      body.complete ? 1 : 0,
      body.notes ?? null,
    ],
  )
  return getOrderById(Number(result.insertId))
}

export async function getOrderById(orderId) {
  const [rows] = await getPool().query(`${ORDER_SELECT} WHERE o.id = ?`, [orderId])
  if (!rows.length) throw new HttpError(404, 'Order not found')
  return mapOrder(rows[0])
}

export async function deleteOrder(orderId) {
  if (orderId >= 1000) throw new HttpError(400, 'Invalid ID supplied')
  const [result] = await getPool().query('DELETE FROM orders WHERE id = ?', [orderId])
  if (result.affectedRows === 0) throw new HttpError(404, 'Order not found')
}
