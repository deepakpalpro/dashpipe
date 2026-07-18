import { createApp } from './app.js'

const host = process.env.HOST || '0.0.0.0'
const port = Number(process.env.PORT || 4011)

const app = createApp()
app.listen(port, host, () => {
  console.log(`petstore-inventory listening on http://${host}:${port}`)
  console.log('  GET /health')
  console.log('  GET /api/v3/inventory/items')
  console.log('  GET /api/v3/inventory/items/:sku')
  console.log('  GET /api/v3/inventory/summary')
})
