import { describe, expect, it } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { PipeletsCatalogPage } from './PipeletsCatalogPage'
import { renderWithProviders } from '../../test/renderWithProviders'
import type { PipeletCatalogEntry } from './catalogFilter'

const CATALOG: PipeletCatalogEntry[] = [
  {
    id: 'plet-rest-source',
    name: 'REST Source',
    category: 'Source',
    version: '1.0.0',
    runtime: 'Java',
    description: 'HTTP',
    active: false,
  },
  {
    id: 'plet-json-transform',
    name: 'JSON Transform',
    category: 'Processor',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Map',
    active: true,
  },
  {
    id: 'plet-s3-destination',
    name: 'S3 Destination',
    category: 'Destination',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Write',
    active: false,
  },
]

describe('PipeletsCatalog', () => {
  it('renders active filter by default', () => {
    renderWithProviders(<PipeletsCatalogPage catalog={CATALOG} />)
    expect(screen.getAllByTestId('pipelet-card')).toHaveLength(1)
    expect(screen.getByText('JSON Transform')).toBeInTheDocument()
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('Showing 1 of 3')
    expect(screen.getByRole('radio', { name: 'Active' })).toBeChecked()
  })

  it('reduces card count when category filter applied', async () => {
    const user = userEvent.setup()
    renderWithProviders(<PipeletsCatalogPage catalog={CATALOG} />)

    await user.click(
      within(screen.getByRole('radiogroup', { name: 'Pipelet status' })).getByRole(
        'radio',
        { name: 'Inactive' },
      ),
    )
    await user.click(screen.getByRole('tab', { name: 'Source' }))
    expect(screen.getAllByTestId('pipelet-card')).toHaveLength(1)
    expect(screen.getByText('REST Source')).toBeInTheDocument()
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('Showing 1 of 3')
  })

  it('filters by active / inactive status', async () => {
    const user = userEvent.setup()
    renderWithProviders(<PipeletsCatalogPage catalog={CATALOG} />)

    const statusGroup = screen.getByRole('radiogroup', { name: 'Pipelet status' })
    expect(within(statusGroup).queryByRole('radio', { name: 'All' })).not.toBeInTheDocument()
    expect(within(statusGroup).getByRole('radio', { name: 'Active' })).toBeChecked()
    expect(screen.getAllByTestId('pipelet-card')).toHaveLength(1)

    await user.click(within(statusGroup).getByRole('radio', { name: 'Inactive' }))
    expect(screen.getAllByTestId('pipelet-card')).toHaveLength(2)
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('Showing 2 of 3')
  })

  it('shows Register for admin stub session', () => {
    renderWithProviders(<PipeletsCatalogPage catalog={CATALOG} />)
    expect(
      screen.getByRole('button', { name: 'Register Pipelet' }),
    ).toBeInTheDocument()
  })

  it('opens pipelet details in an overlay dialog', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <PipeletsCatalogPage
        catalog={[
          {
            ...CATALOG[1],
            configSchemaPreview: { type: 'object', required: ['url'] },
            deploymentConfiguration: { region: 'us-east-1' },
          },
        ]}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Details' }))

    const dialog = screen.getByRole('dialog', { name: 'Pipelet detail' })
    expect(dialog).toBeInTheDocument()
    expect(dialog).toHaveTextContent('JSON Transform')
    expect(dialog).toHaveTextContent('Map')
    expect(dialog).toHaveTextContent('Active')
    expect(dialog).toHaveTextContent('Config schema')
    expect(dialog).toHaveTextContent('Deployment configuration')

    await user.click(screen.getByRole('button', { name: 'Close' }))
    expect(
      screen.queryByRole('dialog', { name: 'Pipelet detail' }),
    ).not.toBeInTheDocument()
  })
})
