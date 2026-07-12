import { createApp } from './app.js'
import { config } from './config.js'
import { closePool, pingDb } from './db.js'

async function main() {
  await pingDb()
  const app = createApp()
  const server = app.listen(config.port, config.host, () => {
    console.log(`Petstore API listening on http://${config.host}:${config.port}`)
    console.log(`OpenAPI base path: /api/v3`)
    console.log(`Health: http://127.0.0.1:${config.port}/health`)
  })

  const shutdown = async (signal) => {
    console.log(`Received ${signal}, shutting down…`)
    server.close(async () => {
      await closePool()
      process.exit(0)
    })
  }
  process.on('SIGINT', () => shutdown('SIGINT'))
  process.on('SIGTERM', () => shutdown('SIGTERM'))
}

main().catch((err) => {
  console.error('Failed to start Petstore API:', err.message)
  process.exit(1)
})
