import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import mysql from 'mysql2/promise'
import { config } from './config.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const sqlDir = path.resolve(__dirname, '../sql')

const SCHEMA_FILES = ['01_schema.sql', '03_inventory.sql']
const SEED_FILES = ['02_seed.sql', '04_inventory_seed.sql', '06_orders_invoice_seed.sql']

async function runSqlFile(connection, fileName) {
  const fullPath = path.join(sqlDir, fileName)
  const sql = await fs.readFile(fullPath, 'utf8')
  await connection.query(sql)
  console.log(`Applied ${fileName}`)
}

async function ensureDatabase() {
  const connection = await mysql.createConnection({
    host: config.mysql.host,
    port: config.mysql.port,
    user: config.mysql.rootUser,
    password: config.mysql.rootPassword,
    multipleStatements: true,
  })
  try {
    const db = config.mysql.database.replace(/[^a-zA-Z0-9_]/g, '')
    const user = config.mysql.user.replace(/[^a-zA-Z0-9_]/g, '')
    await connection.query(
      `CREATE DATABASE IF NOT EXISTS \`${db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`,
    )
    await connection.query(`GRANT ALL PRIVILEGES ON \`${db}\`.* TO '${user}'@'%'`)
    await connection.query('FLUSH PRIVILEGES')
    console.log(`Database '${db}' ready`)
  } finally {
    await connection.end()
  }
}

async function ensureOrderInvoiceColumns(connection) {
  const columns = [
    ['customer_username', "VARCHAR(255) NULL AFTER pet_id"],
    ['unit_price', 'DECIMAL(12, 2) NOT NULL DEFAULT 0.00 AFTER quantity'],
    ['currency', "CHAR(3) NOT NULL DEFAULT 'USD' AFTER unit_price"],
    ['notes', 'VARCHAR(512) NULL AFTER complete'],
  ]
  for (const [name, ddl] of columns) {
    const [rows] = await connection.query(
      `SELECT COUNT(*) AS c FROM information_schema.COLUMNS
       WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'orders' AND COLUMN_NAME = ?`,
      [config.mysql.database, name],
    )
    if (Number(rows[0].c) === 0) {
      await connection.query(`ALTER TABLE orders ADD COLUMN ${name} ${ddl}`)
      console.log(`Added orders.${name}`)
    }
  }
}

async function migrate() {
  await ensureDatabase()

  const connection = await mysql.createConnection({
    host: config.mysql.host,
    port: config.mysql.port,
    user: config.mysql.user,
    password: config.mysql.password,
    database: config.mysql.database,
    multipleStatements: true,
  })
  try {
    for (const file of SCHEMA_FILES) {
      await runSqlFile(connection, file)
    }
    await ensureOrderInvoiceColumns(connection)
    if (process.argv.includes('--seed')) {
      for (const file of SEED_FILES) {
        await runSqlFile(connection, file)
      }
    }
    console.log('Migration complete')
  } finally {
    await connection.end()
  }
}

migrate().catch((err) => {
  console.error('Migration failed:', err.message)
  process.exit(1)
})
