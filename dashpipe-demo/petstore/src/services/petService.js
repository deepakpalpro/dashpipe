import { getPool, withTransaction } from '../db.js'
import { HttpError } from '../http.js'

const PET_STATUSES = new Set(['available', 'pending', 'sold'])

async function mapPetRows(rows) {
  if (!rows.length) return []

  const ids = rows.map((r) => r.id)
  const pool = getPool()
  const [photos] = await pool.query(
    `SELECT pet_id, url FROM pet_photos WHERE pet_id IN (${ids.map(() => '?').join(',')}) ORDER BY id`,
    ids,
  )
  const [tagRows] = await pool.query(
    `SELECT pt.pet_id, t.id, t.name
     FROM pet_tags pt
     JOIN tags t ON t.id = pt.tag_id
     WHERE pt.pet_id IN (${ids.map(() => '?').join(',')})
     ORDER BY t.id`,
    ids,
  )

  const photosByPet = new Map()
  for (const p of photos) {
    if (!photosByPet.has(p.pet_id)) photosByPet.set(p.pet_id, [])
    photosByPet.get(p.pet_id).push(p.url)
  }
  const tagsByPet = new Map()
  for (const t of tagRows) {
    if (!tagsByPet.has(t.pet_id)) tagsByPet.set(t.pet_id, [])
    tagsByPet.get(t.pet_id).push({ id: Number(t.id), name: t.name })
  }

  return rows.map((r) => ({
    id: Number(r.id),
    name: r.name,
    status: r.status,
    category: r.category_id
      ? { id: Number(r.category_id), name: r.category_name }
      : undefined,
    photoUrls: photosByPet.get(r.id) ?? [],
    tags: tagsByPet.get(r.id) ?? [],
  }))
}

async function ensureCategory(conn, category) {
  if (!category) return null
  if (category.id != null) {
    const [rows] = await conn.query('SELECT id, name FROM categories WHERE id = ?', [category.id])
    if (rows.length) {
      if (category.name && category.name !== rows[0].name) {
        await conn.query('UPDATE categories SET name = ? WHERE id = ?', [category.name, category.id])
      }
      return Number(rows[0].id)
    }
  }
  if (!category.name) return null
  const [existing] = await conn.query('SELECT id FROM categories WHERE name = ?', [category.name])
  if (existing.length) return Number(existing[0].id)
  const [result] = await conn.query('INSERT INTO categories (name) VALUES (?)', [category.name])
  return Number(result.insertId)
}

async function ensureTags(conn, tags = []) {
  const ids = []
  for (const tag of tags) {
    if (tag.id != null) {
      const [rows] = await conn.query('SELECT id FROM tags WHERE id = ?', [tag.id])
      if (rows.length) {
        if (tag.name) {
          await conn.query('UPDATE tags SET name = ? WHERE id = ?', [tag.name, tag.id])
        }
        ids.push(Number(rows[0].id))
        continue
      }
    }
    if (!tag.name) continue
    const [existing] = await conn.query('SELECT id FROM tags WHERE name = ?', [tag.name])
    if (existing.length) {
      ids.push(Number(existing[0].id))
    } else {
      const [result] = await conn.query('INSERT INTO tags (name) VALUES (?)', [tag.name])
      ids.push(Number(result.insertId))
    }
  }
  return ids
}

async function replacePhotos(conn, petId, photoUrls = []) {
  await conn.query('DELETE FROM pet_photos WHERE pet_id = ?', [petId])
  for (const url of photoUrls) {
    await conn.query('INSERT INTO pet_photos (pet_id, url) VALUES (?, ?)', [petId, url])
  }
}

async function replaceTags(conn, petId, tagIds) {
  await conn.query('DELETE FROM pet_tags WHERE pet_id = ?', [petId])
  for (const tagId of tagIds) {
    await conn.query('INSERT INTO pet_tags (pet_id, tag_id) VALUES (?, ?)', [petId, tagId])
  }
}

export async function getPetById(petId) {
  const [rows] = await getPool().query(
    `SELECT p.id, p.name, p.status, p.category_id, c.name AS category_name
     FROM pets p
     LEFT JOIN categories c ON c.id = p.category_id
     WHERE p.id = ?`,
    [petId],
  )
  if (!rows.length) throw new HttpError(404, 'Pet not found')
  const [pet] = await mapPetRows(rows)
  return pet
}

export async function findPetsByStatus(status) {
  if (!PET_STATUSES.has(status)) throw new HttpError(400, 'Invalid status value')
  const [rows] = await getPool().query(
    `SELECT p.id, p.name, p.status, p.category_id, c.name AS category_name
     FROM pets p
     LEFT JOIN categories c ON c.id = p.category_id
     WHERE p.status = ?
     ORDER BY p.id`,
    [status],
  )
  return mapPetRows(rows)
}

export async function findPetsByTags(tags) {
  if (!tags?.length) throw new HttpError(400, 'Invalid tag value')
  const [rows] = await getPool().query(
    `SELECT DISTINCT p.id, p.name, p.status, p.category_id, c.name AS category_name
     FROM pets p
     LEFT JOIN categories c ON c.id = p.category_id
     JOIN pet_tags pt ON pt.pet_id = p.id
     JOIN tags t ON t.id = pt.tag_id
     WHERE t.name IN (${tags.map(() => '?').join(',')})
     ORDER BY p.id`,
    tags,
  )
  return mapPetRows(rows)
}

export async function addPet(body) {
  if (!body?.name || !Array.isArray(body.photoUrls)) {
    throw new HttpError(422, 'Validation exception: name and photoUrls are required')
  }
  if (body.status && !PET_STATUSES.has(body.status)) {
    throw new HttpError(400, 'Invalid input')
  }

  const petId = await withTransaction(async (conn) => {
    const categoryId = await ensureCategory(conn, body.category)
    const tagIds = await ensureTags(conn, body.tags ?? [])
    const [result] = await conn.query(
      'INSERT INTO pets (name, category_id, status) VALUES (?, ?, ?)',
      [body.name, categoryId, body.status ?? 'available'],
    )
    const id = Number(result.insertId)
    await replacePhotos(conn, id, body.photoUrls)
    await replaceTags(conn, id, tagIds)
    return id
  })

  return getPetById(petId)
}

export async function updatePet(body) {
  if (body?.id == null) throw new HttpError(400, 'Invalid ID supplied')
  if (!body?.name || !Array.isArray(body.photoUrls)) {
    throw new HttpError(422, 'Validation exception: name and photoUrls are required')
  }
  if (body.status && !PET_STATUSES.has(body.status)) {
    throw new HttpError(400, 'Invalid input')
  }

  await withTransaction(async (conn) => {
    const [existing] = await conn.query('SELECT id FROM pets WHERE id = ?', [body.id])
    if (!existing.length) throw new HttpError(404, 'Pet not found')

    const categoryId = await ensureCategory(conn, body.category)
    const tagIds = await ensureTags(conn, body.tags ?? [])
    await conn.query('UPDATE pets SET name = ?, category_id = ?, status = ? WHERE id = ?', [
      body.name,
      categoryId,
      body.status ?? 'available',
      body.id,
    ])
    await replacePhotos(conn, body.id, body.photoUrls)
    await replaceTags(conn, body.id, tagIds)
  })

  return getPetById(body.id)
}

export async function updatePetWithForm(petId, { name, status }) {
  const [existing] = await getPool().query('SELECT id FROM pets WHERE id = ?', [petId])
  if (!existing.length) throw new HttpError(400, 'Invalid input')
  if (status && !PET_STATUSES.has(status)) throw new HttpError(400, 'Invalid input')

  const fields = []
  const values = []
  if (name !== undefined) {
    fields.push('name = ?')
    values.push(name)
  }
  if (status !== undefined) {
    fields.push('status = ?')
    values.push(status)
  }
  if (fields.length) {
    values.push(petId)
    await getPool().query(`UPDATE pets SET ${fields.join(', ')} WHERE id = ?`, values)
  }
  return getPetById(petId)
}

export async function deletePet(petId) {
  const [result] = await getPool().query('DELETE FROM pets WHERE id = ?', [petId])
  if (result.affectedRows === 0) throw new HttpError(400, 'Invalid pet value')
}

export async function uploadImage(petId, { additionalMetadata, buffer }) {
  const [existing] = await getPool().query('SELECT id FROM pets WHERE id = ?', [petId])
  if (!existing.length) throw new HttpError(404, 'Pet not found')
  if (!buffer?.length) throw new HttpError(400, 'No file uploaded')

  const url = `uploads/pet-${petId}-${Date.now()}.bin`
  await getPool().query('INSERT INTO pet_photos (pet_id, url) VALUES (?, ?)', [petId, url])

  return {
    code: 200,
    type: 'unknown',
    message: additionalMetadata
      ? `additionalMetadata: ${additionalMetadata}\n\nFile uploaded to ${url}, ${buffer.length} bytes`
      : `File uploaded to ${url}, ${buffer.length} bytes`,
  }
}
