export class HttpError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
    this.name = 'HttpError'
  }
}

export function asyncHandler(fn) {
  return (req, res, next) => {
    Promise.resolve(fn(req, res, next)).catch(next)
  }
}

export function parseId(value, label = 'id') {
  const id = Number(value)
  if (!Number.isInteger(id)) {
    throw new HttpError(400, `Invalid ${label}`)
  }
  return id
}

export function requireFields(body, fields) {
  for (const field of fields) {
    if (body?.[field] === undefined || body?.[field] === null || body?.[field] === '') {
      throw new HttpError(422, `Validation exception: '${field}' is required`)
    }
  }
}
