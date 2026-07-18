import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { HeartbeatPanel } from './HeartbeatPanel'

describe('HeartbeatPanel', () => {
  it('renders last-seen timestamp from mock heartbeat', () => {
    render(
      <HeartbeatPanel
        data={{
          pipeline_id: 'pipe-1',
          tenant_id: 'T001',
          last_heartbeat_epoch_seconds: 1_720_000_000,
          stale: false,
        }}
      />,
    )
    expect(screen.getByTestId('heartbeat-last-seen')).toHaveTextContent(
      'Last seen:',
    )
    expect(screen.getByTestId('heartbeat-status')).toHaveTextContent('Healthy')
  })
})
