import express from 'express'
import { pingDb } from './db.js'
import { HttpError } from './http.js'
import { inventoryRouter } from './routes/inventory.js'
import { petRouter } from './routes/pet.js'
import { storeRouter } from './routes/store.js'
import { userRouter } from './routes/user.js'

export function createApp() {
  const app = express()

  app.use(express.json({ limit: '10mb' }))
  app.use(express.urlencoded({ extended: true }))

  app.get('/health', async (_req, res, next) => {
    try {
      const dbOk = await pingDb()
      res.json({ status: dbOk ? 'UP' : 'DOWN', database: dbOk ? 'UP' : 'DOWN' })
    } catch (err) {
      next(err)
    }
  })

  // OpenAPI servers.url is /api/v3
  const api = express.Router()
  api.use('/pet', petRouter)
  api.use('/store', storeRouter)
  api.use('/user', userRouter)
  api.use('/inventory', inventoryRouter)
  app.use('/api/v3', api)

  app.use((err, _req, res, _next) => {
    const status = err instanceof HttpError ? err.status : err.status || 500
    if (status >= 500) {
      console.error(err)
    }
    res.status(status).json({
      code: status,
      type: err.name || 'Error',
      message: err.message || 'Unexpected error',
    })
  })

  return app
}
