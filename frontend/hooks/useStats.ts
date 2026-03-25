import useSWR from 'swr'
import { fetcher } from '../lib/api'

export interface Stats {
  totalActiveAirdrops: number
  totalEstimatedValueMin: number
  totalEstimatedValueMax: number
  endingTodayCount: number
}

export function useStats() {
  const { data, error, isLoading } = useSWR<Stats>(
    '/stats',
    fetcher,
    { refreshInterval: 30000 }
  )

  return {
    stats: data,
    isLoading,
    isError: error
  }
}
