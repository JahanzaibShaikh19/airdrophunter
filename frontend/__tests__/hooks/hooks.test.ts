/**
 * Hooks tests for useAirdrops and useStats.
 *
 * We mock the `swr` module so these tests are pure unit tests with no
 * real network/SWR cache involvement.
 */

import { renderHook } from '@testing-library/react'

jest.mock('swr')
import useSWR from 'swr'

const mockUseSWR = useSWR as jest.MockedFunction<typeof useSWR>

// ── useAirdrops ──────────────────────────────────────────────────────────────

import { useAirdrops, useHotAirdrops } from '@/hooks/useAirdrops'
import type { Airdrop } from '@/lib/types'

const sampleAirdrop: Airdrop = {
  id: 1,
  name: 'LayerZero Airdrop',
  description: 'Test desc',
  token: 'ZRO',
  protocol: 'LayerZero',
  chain: 'Multi-chain',
  estimatedValue: 2500,
  endsAt: new Date().toISOString(),
  isActive: true,
  websiteUrl: null,
  createdAt: new Date().toISOString(),
}

describe('useAirdrops()', () => {
  afterEach(() => jest.clearAllMocks())

  it('returns airdrops when SWR resolves data', () => {
    mockUseSWR.mockReturnValue({
      data: [sampleAirdrop],
      error: undefined,
      isLoading: false,
    } as ReturnType<typeof useSWR>)

    const { result } = renderHook(() => useAirdrops())
    expect(result.current.airdrops).toHaveLength(1)
    expect(result.current.airdrops[0].name).toBe('LayerZero Airdrop')
    expect(result.current.isLoading).toBe(false)
    expect(result.current.error).toBeUndefined()
  })

  it('returns empty array when SWR data is undefined (loading state)', () => {
    mockUseSWR.mockReturnValue({
      data: undefined,
      error: undefined,
      isLoading: true,
    } as ReturnType<typeof useSWR>)

    const { result } = renderHook(() => useAirdrops())
    expect(result.current.airdrops).toEqual([])
    expect(result.current.isLoading).toBe(true)
  })

  it('passes error through when SWR errors', () => {
    const err = new Error('Network failure')
    mockUseSWR.mockReturnValue({
      data: undefined,
      error: err,
      isLoading: false,
    } as ReturnType<typeof useSWR>)

    const { result } = renderHook(() => useAirdrops())
    expect(result.current.error).toEqual(err)
    expect(result.current.airdrops).toEqual([])
  })

  it('calls SWR with refreshInterval of 30000', () => {
    mockUseSWR.mockReturnValue({ data: [], error: undefined, isLoading: false } as ReturnType<typeof useSWR>)
    renderHook(() => useAirdrops())
    expect(mockUseSWR).toHaveBeenCalledWith(
      expect.stringContaining('/api/airdrops'),
      expect.any(Function),
      expect.objectContaining({ refreshInterval: 30_000 })
    )
  })
})

describe('useHotAirdrops()', () => {
  afterEach(() => jest.clearAllMocks())

  it('calls SWR with /api/airdrops/hot URL', () => {
    mockUseSWR.mockReturnValue({ data: [], error: undefined, isLoading: false } as ReturnType<typeof useSWR>)
    renderHook(() => useHotAirdrops())
    expect(mockUseSWR).toHaveBeenCalledWith(
      expect.stringContaining('/api/airdrops/hot'),
      expect.any(Function),
      expect.objectContaining({ refreshInterval: 30_000 })
    )
  })

  it('returns sorted airdrops from SWR', () => {
    const drops = [
      { ...sampleAirdrop, id: 1, estimatedValue: 4200 },
      { ...sampleAirdrop, id: 2, estimatedValue: 2500 },
    ]
    mockUseSWR.mockReturnValue({ data: drops, error: undefined, isLoading: false } as ReturnType<typeof useSWR>)

    const { result } = renderHook(() => useHotAirdrops())
    expect(result.current.airdrops).toHaveLength(2)
    expect(result.current.airdrops[0].estimatedValue).toBe(4200)
  })
})

// ── useStats ─────────────────────────────────────────────────────────────────

import { useStats } from '@/hooks/useStats'
import type { StatsDto } from '@/lib/types'

const sampleStats: StatsDto = {
  totalActive: 10,
  totalEstimatedValue: 16340,
  endingToday: 2,
}

describe('useStats()', () => {
  afterEach(() => jest.clearAllMocks())

  it('returns stats when SWR resolves data', () => {
    mockUseSWR.mockReturnValue({ data: sampleStats, error: undefined, isLoading: false } as ReturnType<typeof useSWR>)

    const { result } = renderHook(() => useStats())
    expect(result.current.stats).toEqual(sampleStats)
    expect(result.current.isLoading).toBe(false)
  })

  it('returns null stats when loading', () => {
    mockUseSWR.mockReturnValue({ data: undefined, error: undefined, isLoading: true } as ReturnType<typeof useSWR>)

    const { result } = renderHook(() => useStats())
    expect(result.current.stats).toBeNull()
    expect(result.current.isLoading).toBe(true)
  })

  it('calls SWR with /api/stats URL and 30s refresh', () => {
    mockUseSWR.mockReturnValue({ data: null, error: undefined, isLoading: false } as ReturnType<typeof useSWR>)
    renderHook(() => useStats())
    expect(mockUseSWR).toHaveBeenCalledWith(
      expect.stringContaining('/api/stats'),
      expect.any(Function),
      expect.objectContaining({ refreshInterval: 30_000 })
    )
  })
})
