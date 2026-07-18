import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // Bind IPv4 so localdev wait_http(http://localhost:5173) works on macOS.
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    // Forward /api to dashflow-api when running with `npm run dev:api` (MSW off).
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/actuator': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
  },
})
