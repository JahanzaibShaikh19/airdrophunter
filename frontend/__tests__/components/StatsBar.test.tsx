import React from 'react'
import { render, screen } from '@testing-library/react'

// Mock the useStats hook before importing the component
jest.mock('@/hooks/useStats', () => ({
  useStats: jest.fn(),
}))

import { useStats } from '@/hooks/useStats'
import { StatsBar } from '@/components/StatsBar'

const mockUseStats = useStats as jest.MockedFunction<typeof useStats>

describe('StatsBar', () => {
  afterEach(() => jest.clearAllMocks())

  it('renders loading skeletons when isLoading is true', () => {
    mockUseStats.mockReturnValue({ stats: null, isLoading: true, error: undefined })
    const { container } = render(<StatsBar />)
    // Three skeleton divs should be present
    const skeletons = container.querySelectorAll('.animate-pulse > div')
    expect(skeletons.length).toBeGreaterThanOrEqual(3)
  })

  it('renders nothing when stats is null and not loading', () => {
    mockUseStats.mockReturnValue({ stats: null, isLoading: false, error: undefined })
    const { container } = render(<StatsBar />)
    expect(container.firstChild).toBeNull()
  })

  it('renders total active count', () => {
    mockUseStats.mockReturnValue({
      stats: { totalActive: 10, totalEstimatedValue: 16340, endingToday: 2 },
      isLoading: false,
      error: undefined,
    })
    render(<StatsBar />)
    expect(screen.getByText('10')).toBeInTheDocument()
  })

  it('renders formatted total estimated value', () => {
    mockUseStats.mockReturnValue({
      stats: { totalActive: 5, totalEstimatedValue: 16340, endingToday: 1 },
      isLoading: false,
      error: undefined,
    })
    render(<StatsBar />)
    expect(screen.getByText(/\$16,340/)).toBeInTheDocument()
  })

  it('renders ending today count', () => {
    mockUseStats.mockReturnValue({
      stats: { totalActive: 5, totalEstimatedValue: 5000, endingToday: 3 },
      isLoading: false,
      error: undefined,
    })
    render(<StatsBar />)
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('renders all three stat labels', () => {
    mockUseStats.mockReturnValue({
      stats: { totalActive: 1, totalEstimatedValue: 100, endingToday: 0 },
      isLoading: false,
      error: undefined,
    })
    render(<StatsBar />)
    expect(screen.getByText('Active Airdrops')).toBeInTheDocument()
    expect(screen.getByText('Total Est. Value')).toBeInTheDocument()
    expect(screen.getByText('Ending Today')).toBeInTheDocument()
  })
})
