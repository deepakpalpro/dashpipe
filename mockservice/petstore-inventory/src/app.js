import express from 'express'
import { createStore } from './store.js'

export function createApp(store = createStore()) {
  const app = express()
  app.disable('x-powered-by')
  app.use(express.json({ limit: '1mb' }))

  app.get('/health', (_req, res) => {
    res.json({ status: 'UP', service: 'petstore-inventory' })
  })

  app.get('/api/v3/inventory/items', (req, res) => {
    const category = typeof req.query.category === 'string' ? req.query.category : undefined
    const status = typeof req.query.status === 'string' ? req.query.status : undefined
    const items = store.list({ category, status })
    res.json(items)
  })

  app.get('/api/v3/inventory/items/:sku', (req, res) => {
    const item = store.get(req.params.sku)
    if (!item) {
      res.status(404).json({ message: `SKU not found: ${req.params.sku}` })
      return
    }
    res.json(item)
  })

  app.get('/api/v3/inventory/summary', (_req, res) => {
    res.json(store.summary())
  })

  app.use((err, _req, res, _next) => {
    console.error(err)
    res.status(500).json({ message: 'internal error' })
  })

  return app
}
