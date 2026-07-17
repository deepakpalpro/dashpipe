import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RunControls } from './RunControls'

describe('RunControls', () => {
  it('shows Activate when pipeline is draft', async () => {
    const user = userEvent.setup()
    const onActivate = vi.fn()
    render(
      <RunControls
        canRun
        status="DRAFT"
        onDryRun={() => undefined}
        onSave={() => undefined}
        onActivate={onActivate}
        onDeactivate={() => undefined}
        onRun={() => undefined}
      />,
    )

    expect(screen.getByTestId('pipeline-status')).toHaveTextContent('DRAFT')
    const activate = screen.getByRole('button', { name: 'Activate' })
    expect(activate).toBeEnabled()
    await user.click(activate)
    expect(onActivate).toHaveBeenCalledOnce()
  })

  it('shows Deactivate when pipeline is active', async () => {
    const user = userEvent.setup()
    const onDeactivate = vi.fn()
    render(
      <RunControls
        canRun
        status="ACTIVE"
        onDryRun={() => undefined}
        onSave={() => undefined}
        onActivate={() => undefined}
        onDeactivate={onDeactivate}
        onRun={() => undefined}
      />,
    )

    expect(screen.getByTestId('pipeline-status')).toHaveTextContent('ACTIVE')
    expect(screen.queryByRole('button', { name: 'Activate' })).not.toBeInTheDocument()
    const deactivate = screen.getByRole('button', { name: 'Deactivate' })
    expect(deactivate).toBeEnabled()
    await user.click(deactivate)
    expect(onDeactivate).toHaveBeenCalledOnce()
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
        onActivate={() => undefined}
        onDeactivate={() => undefined}
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
        onActivate={() => undefined}
        onDeactivate={() => undefined}
        onRun={() => undefined}
        onForceStop={() => undefined}
      />,
    )

    expect(screen.getByTestId('force-stop')).toBeDisabled()
  })

  it('shows trigger payload editor when enabled', () => {
    render(
      <RunControls
        canRun
        showTriggerPayload
        triggerPayloadText='{"a":1}'
        onTriggerPayloadChange={() => undefined}
        onDryRun={() => undefined}
        onSave={() => undefined}
        onActivate={() => undefined}
        onDeactivate={() => undefined}
        onRun={() => undefined}
      />,
    )
    expect(screen.getByTestId('trigger-payload')).toHaveValue('{"a":1}')
  })
})
