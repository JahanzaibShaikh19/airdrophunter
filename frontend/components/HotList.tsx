'use client'

import { useHotAirdrops } from '@/hooks/useAirdrops'
import { AirdropCard } from './AirdropCard'
import { Flame } from 'lucide-react'

export function HotList() {
  const { airdrops, isLoading } = useHotAirdrops()

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 animate-pulse">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="h-64 rounded-2xl bg-slate-800/60" />
        ))}
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center gap-2 mb-6">
        <Flame className="w-5 h-5 text-orange-400 fill-orange-400" />
        <p className="text-slate-400 text-sm">
          Sorted by highest estimated value — auto-refreshes every 30s
        </p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {airdrops.map((airdrop, i) => (
          <AirdropCard key={airdrop.id} airdrop={airdrop} rank={i + 1} />
        ))}
      </div>
    </div>
  )
}
