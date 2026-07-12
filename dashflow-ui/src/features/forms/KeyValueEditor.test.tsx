import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../test/renderWithProviders'
import { KeyValueEditor } from './KeyValueEditor'

describe('KeyValueEditor', () => {
  it('adds a key/value pair', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    renderWithProviders(
      <KeyValueEditor entries={{}} onChange={onChange} title="Extra" />,
    )

    await user.type(screen.getByLabelText('Extra key'), 'region')
    await user.type(screen.getByLabelText('Extra value'), 'us-east')
    await user.click(screen.getByRole('button', { name: 'Add' }))

    expect(onChange).toHaveBeenCalledWith({ region: 'us-east' })
  })
})
