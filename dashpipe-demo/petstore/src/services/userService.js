import { getPool } from '../db.js'
import { HttpError } from '../http.js'

function mapUser(row) {
  return {
    id: Number(row.id),
    username: row.username,
    firstName: row.first_name ?? undefined,
    lastName: row.last_name ?? undefined,
    email: row.email ?? undefined,
    password: row.password ?? undefined,
    phone: row.phone ?? undefined,
    userStatus: row.user_status != null ? Number(row.user_status) : undefined,
  }
}

async function insertUser(conn, body) {
  if (!body?.username) {
    throw new HttpError(400, 'Invalid input')
  }
  const [result] = await conn.query(
    `INSERT INTO users (username, first_name, last_name, email, password, phone, user_status)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [
      body.username,
      body.firstName ?? null,
      body.lastName ?? null,
      body.email ?? null,
      body.password ?? null,
      body.phone ?? null,
      body.userStatus ?? 0,
    ],
  )
  return Number(result.insertId)
}

export async function createUser(body) {
  const pool = getPool()
  try {
    const id = await insertUser(pool, body)
    return getUserByName(body.username)
  } catch (err) {
    if (err.code === 'ER_DUP_ENTRY') {
      throw new HttpError(400, 'Invalid input')
    }
    throw err
  }
}

export async function createUsersWithList(users) {
  if (!Array.isArray(users) || !users.length) {
    throw new HttpError(400, 'Invalid input')
  }
  const pool = getPool()
  const connection = await pool.getConnection()
  try {
    await connection.beginTransaction()
    let lastUsername
    for (const user of users) {
      await insertUser(connection, user)
      lastUsername = user.username
    }
    await connection.commit()
    return getUserByName(lastUsername)
  } catch (err) {
    await connection.rollback()
    if (err.code === 'ER_DUP_ENTRY') throw new HttpError(400, 'Invalid input')
    throw err
  } finally {
    connection.release()
  }
}

export async function getUserByName(username) {
  const [rows] = await getPool().query('SELECT * FROM users WHERE username = ?', [username])
  if (!rows.length) throw new HttpError(404, 'User not found')
  return mapUser(rows[0])
}

export async function updateUser(username, body) {
  const [existing] = await getPool().query('SELECT id FROM users WHERE username = ?', [username])
  if (!existing.length) throw new HttpError(404, 'user not found')
  if (!body || typeof body !== 'object') throw new HttpError(400, 'bad request')

  await getPool().query(
    `UPDATE users
     SET username = COALESCE(?, username),
         first_name = COALESCE(?, first_name),
         last_name = COALESCE(?, last_name),
         email = COALESCE(?, email),
         password = COALESCE(?, password),
         phone = COALESCE(?, phone),
         user_status = COALESCE(?, user_status)
     WHERE username = ?`,
    [
      body.username ?? null,
      body.firstName ?? null,
      body.lastName ?? null,
      body.email ?? null,
      body.password ?? null,
      body.phone ?? null,
      body.userStatus ?? null,
      username,
    ],
  )
}

export async function deleteUser(username) {
  const [result] = await getPool().query('DELETE FROM users WHERE username = ?', [username])
  if (result.affectedRows === 0) throw new HttpError(404, 'User not found')
}

export async function loginUser(username, password) {
  if (!username || !password) {
    throw new HttpError(400, 'Invalid username/password supplied')
  }
  const [rows] = await getPool().query(
    'SELECT id FROM users WHERE username = ? AND password = ?',
    [username, password],
  )
  if (!rows.length) {
    throw new HttpError(400, 'Invalid username/password supplied')
  }
  return {
    token: `logged-in-user:${username}:${Date.now()}`,
    rateLimit: 5000,
    expiresAfter: new Date(Date.now() + 60 * 60 * 1000).toUTCString(),
  }
}

export async function logoutUser() {
  return { message: 'ok' }
}
