import mysql from 'mysql2/promise'
import { config } from './config.js'

let pool

export function getPool() {
  if (!pool) {
    pool = mysql.createPool({
      host: config.mysql.host,
      port: config.mysql.port,
      user: config.mysql.user,
      password: config.mysql.password,
      database: config.mysql.database,
      waitForConnections: true,
      connectionLimit: 10,
      namedPlaceholders: true,
      timezone: 'Z',
      dateStrings: false,
    })
  }
  return pool
}

export async function withTransaction(fn) {
  const connection = await getPool().getConnection()
  try {
    await connection.beginTransaction()
    const result = await fn(connection)
    await connection.commit()
    return result
  } catch (err) {
    await connection.rollback()
    throw err
  } finally {
    connection.release()
  }
}

export async function pingDb() {
  const [rows] = await getPool().query('SELECT 1 AS ok')
  return rows[0]?.ok === 1
}

export async function closePool() {
  if (pool) {
    await pool.end()
    pool = undefined
  }
}
