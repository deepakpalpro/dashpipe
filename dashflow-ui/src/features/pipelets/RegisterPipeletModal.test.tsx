import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RegisterPipeletModal } from './RegisterPipeletModal'
import { renderWithProviders } from '../../test/renderWithProviders'

describe('RegisterPipeletModal', () => {
  it('requires name and image reference', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()
    renderWithProviders(
      <RegisterPipeletModal open onClose={() => undefined} onSubmit={onSubmit} />,
    )

    await user.click(screen.getByRole('button', { name: 'Register' }))
    expect(
      screen.getByText('Name and image/binary reference are required'),
    ).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits when required fields are filled', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const onClose = vi.fn()
    renderWithProviders(
      <RegisterPipeletModal open onClose={onClose} onSubmit={onSubmit} />,
    )

    await user.type(screen.getByLabelText('Pipelet name'), 'Custom Filter')
    await user.type(
      screen.getByLabelText('Image reference'),
      'registry.example/custom:1',
    )
    await user.click(screen.getByRole('button', { name: 'Register' }))

    expect(onSubmit).toHaveBeenCalledWith({
      mode: 'imagePath',
      name: 'Custom Filter',
      category: 'Processor',
      imageRef: 'registry.example/custom:1',
    })
    expect(onClose).toHaveBeenCalled()
  })
})
