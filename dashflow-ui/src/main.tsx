import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

async function enableMocking() {
  // Dev defaults to MSW so Save/Run work without a live API.
  // Set VITE_ENABLE_MSW=false to hit a real backend.
  const flag = import.meta.env.VITE_ENABLE_MSW
  const useMsw =
    flag === 'true' || (import.meta.env.DEV && flag !== 'false')
  if (!useMsw) {
    return
  }
  const { worker } = await import('./mocks/browser')
  return worker.start({ onUnhandledRequest: 'bypass' })
}

void enableMocking().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
})
