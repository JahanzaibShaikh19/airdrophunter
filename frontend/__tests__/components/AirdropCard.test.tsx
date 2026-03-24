import React from 'react'
import { render, screen } from '@testing-library/react'
import { AirdropCard } from '@/components/AirdropCard'
import type { Airdrop } from '@/lib/types'

// ── Fixture ──────────────────────────────────────────────────────────────────

const futureDate = new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString()
const soonDate   = new Date(Date.now() + 1 * 24 * 60 * 60 * 1000).toISOString()

const baseAirdrop: Airdrop = {
  id: 1,
  name: 'LayerZero Airdrop',
  description: 'Cross-chain messaging protocol rewarding early bridge users.',
  token: 'ZRO',
  protocol: 'LayerZero',
  chain: 'Multi-chain',
  estimatedValue: 2500,
  endsAt: futureDate,
  isActive: true,
  websiteUrl: 'https://layerzero.network',
  createdAt: new Date().toISOString(),
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('AirdropCard', () => {
  it('renders the airdrop name', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.getByText('LayerZero Airdrop')).toBeInTheDocument()
  })

  it('renders the token symbol as a badge', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.getByText('ZRO')).toBeInTheDocument()
  })

  it('renders the protocol name', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.getByText('LayerZero')).toBeInTheDocument()
  })

  it('renders the chain', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.getByText('Multi-chain')).toBeInTheDocument()
  })

  it('renders the description', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.getByText(/Cross-chain messaging/)).toBeInTheDocument()
  })

  it('renders estimated value formatted as currency', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.getByText(/\$2,500/)).toBeInTheDocument()
  })

  it('renders the claim link when websiteUrl is provided', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    const link = screen.getByRole('link', { name: /Claim Airdrop/i })
    expect(link).toHaveAttribute('href', 'https://layerzero.network')
    expect(link).toHaveAttribute('target', '_blank')
  })

  it('does not render claim link when websiteUrl is null', () => {
    render(<AirdropCard airdrop={{ ...baseAirdrop, websiteUrl: null }} />)
    expect(screen.queryByRole('link', { name: /Claim Airdrop/i })).toBeNull()
  })

  it('shows rank badge when rank prop is provided', () => {
    render(<AirdropCard airdrop={baseAirdrop} rank={1} />)
    expect(screen.getByText('#1')).toBeInTheDocument()
  })

  it('does not show rank badge without rank prop', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.queryByText(/#\d/)).toBeNull()
  })

  it('shows "Ending soon" urgency badge when ending within 3 days', () => {
    render(<AirdropCard airdrop={{ ...baseAirdrop, endsAt: soonDate }} />)
    expect(screen.getByText(/Ending soon/i)).toBeInTheDocument()
  })

  it('does not show urgency badge when ending far away', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    expect(screen.queryByText(/Ending soon/i)).toBeNull()
  })

  it('renders countdown text', () => {
    render(<AirdropCard airdrop={baseAirdrop} />)
    // Should show some countdown like "10d Xh left"
    expect(screen.getByText(/\d+d \d+h left/)).toBeInTheDocument()
  })
})
