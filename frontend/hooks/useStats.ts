'use client'

import useSWR from 'swr'
import type { StatsDto } from '@/lib/types'

const API = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export function useStats() {
  const { data, error, isLoading } = useSWR<StatsDto>(
    `${API}/api/stats`,
    fetcher,
    { refreshInterval: 30_000 }
  )
  return { stats: data ?? null, error, isLoading }
}
