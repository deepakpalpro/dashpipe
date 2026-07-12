import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test/renderWithProviders'
import { StepPropertiesPanel } from './StepPropertiesPanel'
import type { PipelineGraphNode } from './pipelineGraphReducer'

const node: PipelineGraphNode = {
  id: 'n2',
  position: { x: 0, y: 0 },
  data: {
    pipeletId: 'plet-json-transform',
    name: 'JSON Transform',
    category: 'Processor',
    connectorIds: [],
    serviceIds: [],
    config: {},
    deploymentConfig: {},
    executionConfig: {},
  },
}

describe('StepPropertiesPanel', () => {
  it('removes the selected step when Remove step is clicked', async () => {
    const user = userEvent.setup()
    const onRemove = vi.fn()
    renderWithProviders(
      <StepPropertiesPanel
        node={node}
        connectors={[]}
        services={[]}
        onChange={() => undefined}
        onRemove={onRemove}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Remove step' }))
    expect(onRemove).toHaveBeenCalledWith('n2')
  })

  it('adds a config key/value via the execution config editor', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    renderWithProviders(
      <StepPropertiesPanel
        node={node}
        connectors={[]}
        services={[]}
        onChange={onChange}
      />,
    )

    await user.type(
      screen.getByLabelText('Execution configuration key'),
      'mapping',
    )
    await user.type(
      screen.getByLabelText('Execution configuration value'),
      'a=1',
    )
    const addButtons = screen.getAllByRole('button', { name: 'Add' })
    await user.click(addButtons[1])

    expect(onChange).toHaveBeenCalledWith('n2', {
      executionConfig: { mapping: 'a=1' },
      config: { mapping: 'a=1' },
    })
  })

  it('picks a connector from the searchable dropdown', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    renderWithProviders(
      <StepPropertiesPanel
        node={node}
        connectors={[
          {
            id: 'conn-plet-rest-source',
            tenantId: 'T001',
            connectorTypeId: 'ct-rest',
            name: 'REST Source (plet-rest-source)',
            config: {},
            status: 'ACTIVE',
            lastTestedAt: null,
            createdAt: '2026-07-01T00:00:00Z',
          },
          {
            id: 'conn-plet-webhook-source',
            tenantId: 'T001',
            connectorTypeId: 'ct-event-listener',
            name: 'Webhook Source (plet-webhook-source)',
            config: {},
            status: 'ACTIVE',
            lastTestedAt: null,
            createdAt: '2026-07-01T00:00:00Z',
          },
        ]}
        services={[]}
        onChange={onChange}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Connector' }))
    await user.type(screen.getByLabelText('Connector search'), 'webhook')
    await user.click(
      screen.getByRole('button', { name: /Webhook Source \(plet-webhook-source\)/ }),
    )

    expect(onChange).toHaveBeenCalledWith('n2', {
      connectorIds: ['conn-plet-webhook-source'],
    })
  })

  it('shows required deployment and execution Keys from catalog', () => {
    renderWithProviders(
      <StepPropertiesPanel
        node={{
          ...node,
          data: {
            ...node.data,
            pipeletId: 'plet-s3-source',
            name: 'S3 Source',
            category: 'Source',
            deploymentConfig: { cloud: 'aws', region: '' },
            executionConfig: { objectKey: '' },
          },
        }}
        catalog={[
          {
            id: 'plet-s3-source',
            name: 'S3 Source',
            category: 'Source',
            version: '1.0.0',
            runtime: 'Python',
            description: 'S3',
            active: true,
            requiredDeploymentKeys: ['region'],
            requiredExecutionKeys: ['objectKey'],
          },
        ]}
        connectors={[]}
        services={[]}
        onChange={() => undefined}
      />,
    )

    expect(screen.getByTestId('required-deployment-keys')).toHaveTextContent(
      'region',
    )
    expect(screen.getByTestId('required-execution-keys')).toHaveTextContent(
      'objectKey',
    )
  })
})
