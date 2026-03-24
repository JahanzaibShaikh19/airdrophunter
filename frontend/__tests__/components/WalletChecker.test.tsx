import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { WalletChecker } from '@/components/WalletChecker'

// ── MSW-free: mock fetch globally ─────────────────────────────────────────────

const mockFetch = jest.fn()

beforeAll(() => {
  global.fetch = mockFetch
})

afterEach(() => {
  mockFetch.mockReset()
})

describe('WalletChecker', () => {
  it('renders the input and button', () => {
    render(<WalletChecker />)
    expect(screen.getByPlaceholderText(/0x\.\.\./i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Check/i })).toBeInTheDocument()
  })

  it('disables the button when input is empty', () => {
    render(<WalletChecker />)
    expect(screen.getByRole('button', { name: /Check/i })).toBeDisabled()
  })

  it('enables the button when address is typed', () => {
    render(<WalletChecker />)
    const input = screen.getByPlaceholderText(/0x\.\.\./i)
    fireEvent.change(input, { target: { value: '0xabc' } })
    expect(screen.getByRole('button', { name: /Check/i })).not.toBeDisabled()
  })

  it('shows eligible result on successful API response', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        address: '0xd8da6bf26964af9d7eed9e03e53415d37aa96045',
        eligible: true,
        reason: 'Wallet is eligible based on on-chain activity.',
        estimatedReward: 275.0,
        eligibleAirdrops: ['LayerZero', 'zkSync Era'],
      }),
    })

    render(<WalletChecker />)
    fireEvent.change(screen.getByPlaceholderText(/0x\.\.\./i), {
      target: { value: '0xd8da6bf26964af9d7eed9e03e53415d37aa96045' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Check/i }))

    await waitFor(() => expect(screen.getByText(/Eligible!/i)).toBeInTheDocument())
    expect(screen.getByText(/\$275/)).toBeInTheDocument()
    expect(screen.getByText('LayerZero')).toBeInTheDocument()
    expect(screen.getByText('zkSync Era')).toBeInTheDocument()
  })

  it('shows ineligible result on non-eligible response', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        address: '0x0000000000000000000000000000000000000001',
        eligible: false,
        reason: 'Wallet does not meet eligibility criteria.',
        estimatedReward: 0,
        eligibleAirdrops: [],
      }),
    })

    render(<WalletChecker />)
    fireEvent.change(screen.getByPlaceholderText(/0x\.\.\./i), {
      target: { value: '0x0000000000000000000000000000000000000001' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Check/i }))

    await waitFor(() => expect(screen.getByText(/Not Eligible/i)).toBeInTheDocument())
    expect(screen.queryByText('LayerZero')).toBeNull()
  })

  it('shows error message when fetch fails', async () => {
    mockFetch.mockRejectedValueOnce(new Error('Network error'))

    render(<WalletChecker />)
    fireEvent.change(screen.getByPlaceholderText(/0x\.\.\./i), {
      target: { value: '0xabc' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Check/i }))

    await waitFor(() => expect(screen.getByText(/Network error/i)).toBeInTheDocument())
  })

  it('shows error when server returns non-ok status', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 500 })

    render(<WalletChecker />)
    fireEvent.change(screen.getByPlaceholderText(/0x\.\.\./i), {
      target: { value: '0xabc' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Check/i }))

    await waitFor(() => expect(screen.getByText(/Server error: 500/i)).toBeInTheDocument())
  })

  it('does not submit when address is only whitespace', () => {
    render(<WalletChecker />)
    const input = screen.getByPlaceholderText(/0x\.\.\./i)
    fireEvent.change(input, { target: { value: '   ' } })
    expect(screen.getByRole('button', { name: /Check/i })).toBeDisabled()
  })
})
