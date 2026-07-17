import { describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ConnectorsPage } from './ConnectorsPage'
import { renderWithProviders } from '../../test/renderWithProviders'

describe('ConnectorsList', () => {
  it('renders fixture connector rows from MSW', async () => {
    renderWithProviders(<ConnectorsPage />, {
      initialEntries: ['/connectors'],
    })

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Orders API/ }),
      ).toBeInTheDocument()
    })
  })

  it('filters connectors by taxonomy tree and search', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ConnectorsPage />, {
      initialEntries: ['/connectors'],
    })

    await waitFor(() => {
      expect(screen.getByTestId('connectors-count')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: 'Source' }))
    await waitFor(() => {
      expect(screen.getByTestId('connectors-path')).toHaveTextContent('Source')
    })

    await user.click(screen.getByRole('button', { name: 'Source Http' }))
    await waitFor(() => {
      expect(screen.getByTestId('connectors-path')).toHaveTextContent(
        'Source · Http',
      )
    })

    await user.type(
      screen.getByLabelText('Search connectors'),
      'plet-webhook-source',
    )

    await waitFor(() => {
      expect(screen.getByTestId('connectors-count').textContent).toMatch(
        /Showing 1 of 1/,
      )
    })
    expect(
      screen.getByRole('button', { name: /Webhook Source/ }),
    ).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Search connectors'))
    await user.click(screen.getByRole('button', { name: 'All connectors' }))

    await waitFor(() => {
      const count = screen.getByTestId('connectors-count').textContent ?? ''
      expect(count).toMatch(/Showing \d+ of \d+/)
      expect(count).not.toMatch(/Showing 0 of 0/)
    })

    const pagination = screen.getByLabelText('Connector pagination')
    expect(
      within(pagination).getByTestId('connectors-page').textContent,
    ).toMatch(/Page 1 of/)
  })
})
