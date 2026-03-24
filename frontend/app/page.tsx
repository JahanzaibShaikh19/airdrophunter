'use client'

import { useAirdrops } from '@/hooks/useAirdrops'
import { AirdropCard } from '@/components/AirdropCard'
import { StatsBar } from '@/components/StatsBar'
import { Zap } from 'lucide-react'

export default function HomePage() {
  const { airdrops, isLoading, error } = useAirdrops()

  return (
    <div className="space-y-8">
      {/* Hero */}
      <div className="text-center space-y-3 py-6">
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-400 text-sm font-medium mb-2">
          <Zap className="w-3.5 h-3.5 fill-brand-400" />
          Live Airdrop Tracker
        </div>
        <h1 className="text-4xl sm:text-5xl font-extrabold text-white tracking-tight">
          Hunt the Best{' '}
          <span className="bg-gradient-to-r from-brand-400 to-accent-400 bg-clip-text text-transparent">
            Crypto Airdrops
          </span>
        </h1>
        <p className="text-slate-400 text-lg max-w-xl mx-auto">
          Track high-value token distributions across all chains. Check your wallet eligibility instantly.
        </p>
      </div>

      {/* Stats */}
      <StatsBar />

      {/* Grid */}
      <div>
        <h2 className="text-xl font-bold text-white mb-5">
          Active Airdrops
          <span className="ml-2 text-slate-500 text-base font-normal">
            ({isLoading ? '…' : airdrops.length})
          </span>
        </h2>

        {error && (
          <div className="rounded-2xl border border-rose-500/40 bg-rose-500/10 p-5 text-rose-300 text-sm">
            Failed to load airdrops. Is the backend running?
          </div>
        )}

        {isLoading && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 animate-pulse">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-64 rounded-2xl bg-slate-800/60" />
            ))}
          </div>
        )}

        {!isLoading && !error && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {airdrops.map(airdrop => (
              <AirdropCard key={airdrop.id} airdrop={airdrop} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
