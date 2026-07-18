import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../test/renderWithProviders'
import { SearchableSelect } from './SearchableSelect'

describe('SearchableSelect', () => {
  it('filters options and selects a match', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    renderWithProviders(
      <SearchableSelect
        label="Connector"
        value=""
        onChange={onChange}
        options={[
          { value: 'a', label: 'Orders API', meta: 'ct-rest' },
          { value: 'b', label: 'Webhook Source', meta: 'ct-event-listener' },
          { value: 'c', label: 'S3 Source', meta: 'ct-storage' },
        ]}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Connector' }))
    await user.type(screen.getByLabelText('Connector search'), 'web')
    expect(screen.getByRole('button', { name: /Webhook Source/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Orders API/ })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Webhook Source/ }))
    expect(onChange).toHaveBeenCalledWith('b')
  })
})
