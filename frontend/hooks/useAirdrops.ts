import useSWR from 'swr'
import { fetcher, getAuthToken } from '../lib/api'

export interface Airdrop {
  id: number
  name: string
  symbol: string
  logoUrl?: string
  estimatedValueMin: number
  estimatedValueMax: number
  status: 'LIVE' | 'SOON' | 'ENDED'
  category: 'L2' | 'BRIDGE' | 'DEFI' | 'AI' | 'OTHER'
  deadline?: string
  steps: string[]
  isHot: boolean
  isPro: boolean
  llamaSlug?: string
}

export function useAirdrops() {
  const token = getAuthToken()
  // Automatically pivot to the /pro endpoint if the user has unlocked it
  const endpoint = token ? '/airdrops/pro' : '/airdrops'
  
  const { data, error, isLoading, mutate } = useSWR<Airdrop[]>(
    endpoint,
    fetcher,
    { 
      refreshInterval: 30000, 
      revalidateOnFocus: true 
    }
  )

  return {
    airdrops: data || [],
    isLoading,
    isError: error,
    mutate
  }
}
