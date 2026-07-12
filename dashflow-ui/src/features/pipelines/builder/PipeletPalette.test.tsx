import { describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { render } from '@testing-library/react'
import type { PipeletCatalogEntry } from '../../pipelets/catalogFilter'
import { PipeletPalette, PALETTE_PREVIEW_LIMIT } from './PipeletPalette'

function makeCatalog(): PipeletCatalogEntry[] {
  const items: PipeletCatalogEntry[] = []
  for (let i = 1; i <= 8; i++) {
    items.push({
      id: `plet-src-${i}`,
      name: `Source ${i}`,
      category: 'Source',
      version: '1.0.0',
      runtime: 'Java',
      description: `Source pipelet ${i}`,
      active: true,
    })
  }
  for (let i = 1; i <= 7; i++) {
    items.push({
      id: `plet-proc-${i}`,
      name: `Processor ${i}`,
      category: 'Processor',
      version: '1.0.0',
      runtime: 'Java',
      description: `Processor pipelet ${i}`,
      active: true,
    })
  }
  for (let i = 1; i <= 6; i++) {
    items.push({
      id: `plet-dst-${i}`,
      name: `Destination ${i}`,
      category: 'Destination',
      version: '1.0.0',
      runtime: 'Java',
      description: `Destination pipelet ${i}`,
      active: true,
    })
  }
  items.push({
    id: 'plet-kafka-source',
    name: 'Kafka Source',
    category: 'Source',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Consume Kafka topics',
    active: true,
  })
  items.push({
    id: 'plet-inactive-source',
    name: 'Inactive Source',
    category: 'Source',
    version: '1.0.0',
    runtime: 'Java',
    description: 'Should not appear in palette',
    active: false,
  })
  return items
}

describe('PipeletPalette', () => {
  it('groups by category and shows only five until expanded', async () => {
    const user = userEvent.setup()
    const onAdd = vi.fn()
    render(<PipeletPalette items={makeCatalog()} onAdd={onAdd} />)

    const sourceSection = screen.getByLabelText('Source pipelets')
    // 5 preview items + "Show N more"
    expect(within(sourceSection).getAllByRole('button')).toHaveLength(
      PALETTE_PREVIEW_LIMIT + 1,
    )
    expect(within(sourceSection).queryByText('Source 6')).not.toBeInTheDocument()

    await user.click(
      within(sourceSection).getByRole('button', { name: /Show 4 more/ }),
    )
    expect(within(sourceSection).getByText('Source 6')).toBeInTheDocument()
    expect(
      within(sourceSection).getByRole('button', { name: 'Show less' }),
    ).toBeInTheDocument()
  })

  it('search filters across categories and expands matches', async () => {
    const user = userEvent.setup()
    const onAdd = vi.fn()
    render(<PipeletPalette items={makeCatalog()} onAdd={onAdd} />)

    await user.type(screen.getByLabelText('Search pipelets'), 'Kafka')

    expect(screen.getByText('Kafka Source')).toBeInTheDocument()
    expect(screen.queryByText('Source 1')).not.toBeInTheDocument()
    expect(screen.queryByText(/Show .* more/)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Kafka Source/ }))
    expect(onAdd).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'plet-kafka-source' }),
    )
  })

  it('shows empty state when search has no matches', async () => {
    const user = userEvent.setup()
    render(<PipeletPalette items={makeCatalog()} onAdd={vi.fn()} />)

    await user.type(screen.getByLabelText('Search pipelets'), 'zzzz-none')
    expect(screen.getByText(/No pipelets match/)).toBeInTheDocument()
  })

  it('hides inactive pipelets from the palette', () => {
    render(<PipeletPalette items={makeCatalog()} onAdd={vi.fn()} />)
    expect(screen.queryByText('Inactive Source')).not.toBeInTheDocument()
  })

  it('shows empty guidance when catalog has no active pipelets', () => {
    render(
      <PipeletPalette
        items={[
          {
            id: 'plet-x',
            name: 'Only Inactive',
            category: 'Source',
            version: '1.0.0',
            runtime: 'Java',
            description: 'n/a',
            active: false,
          },
        ]}
        onAdd={vi.fn()}
      />,
    )
    expect(screen.getByText(/No active pipelets/)).toBeInTheDocument()
  })
})
