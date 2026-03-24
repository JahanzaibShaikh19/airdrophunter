export interface Airdrop {
  id: number
  name: string
  description: string
  token: string
  protocol: string
  chain: string
  estimatedValue: number
  endsAt: string
  isActive: boolean
  websiteUrl: string | null
  createdAt: string
}

export interface WalletCheckResponse {
  address: string
  eligible: boolean
  reason: string
  estimatedReward: number
  eligibleAirdrops: string[]
}

export interface StatsDto {
  totalActive: number
  totalEstimatedValue: number
  endingToday: number
}
