'use client'

import useSWR from 'swr'
import type { Airdrop } from '@/lib/types'

const API = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

const fetcher = (url: string) => fetch(url).then((r) => r.json())

export function useAirdrops() {
  const { data, error, isLoading } = useSWR<Airdrop[]>(
    `${API}/api/airdrops`,
    fetcher,
    { refreshInterval: 30_000 }
  )
  return { airdrops: data ?? [], error, isLoading }
}

export function useHotAirdrops() {
  const { data, error, isLoading } = useSWR<Airdrop[]>(
    `${API}/api/airdrops/hot`,
    fetcher,
    { refreshInterval: 30_000 }
  )
  return { airdrops: data ?? [], error, isLoading }
}
