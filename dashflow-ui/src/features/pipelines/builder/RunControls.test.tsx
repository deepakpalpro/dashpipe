import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RunControls } from './RunControls'

describe('RunControls', () => {
  it('shows Deploy when pipeline is draft', async () => {
    const user = userEvent.setup()
    const onDeploy = vi.fn()
    render(
      <RunControls
        canRun
        status="DRAFT"
        onDryRun={() => undefined}
        onSave={() => undefined}
        onDeploy={onDeploy}
        onRun={() => undefined}
      />,
    )

    expect(screen.getByTestId('pipeline-status')).toHaveTextContent('DRAFT')
    const deploy = screen.getByRole('button', { name: 'Deploy' })
    expect(deploy).toBeEnabled()
    await user.click(deploy)
    expect(onDeploy).toHaveBeenCalledOnce()
  })

  it('disables Deploy when already active', () => {
    render(
      <RunControls
        canRun
        status="ACTIVE"
        onDryRun={() => undefined}
        onSave={() => undefined}
        onDeploy={() => undefined}
        onRun={() => undefined}
      />,
    )

    expect(screen.getByTestId('pipeline-status')).toHaveTextContent('ACTIVE')
    expect(screen.getByRole('button', { name: 'Deployed' })).toBeDisabled()
  })

  it('enables Force Stop while running', async () => {
    const user = userEvent.setup()
    const onForceStop = vi.fn()
    render(
      <RunControls
        canRun
        running
        canForceStop
        status="ACTIVE"
        onDryRun={() => undefined}
        onSave={() => undefined}
        onDeploy={() => undefined}
        onRun={() => undefined}
        onForceStop={onForceStop}
      />,
    )

    const stop = screen.getByTestId('force-stop')
    expect(stop).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Running…' })).toBeDisabled()
    await user.click(stop)
    expect(onForceStop).toHaveBeenCalledOnce()
  })

  it('disables Force Stop when nothing live', () => {
    render(
      <RunControls
        canRun
        canForceStop={false}
        status="ACTIVE"
        onDryRun={() => undefined}
        onSave={() => undefined}
        onDeploy={() => undefined}
        onRun={() => undefined}
        onForceStop={() => undefined}
      />,
    )

    expect(screen.getByTestId('force-stop')).toBeDisabled()
  })
})
