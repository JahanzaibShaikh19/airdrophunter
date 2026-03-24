'use client'

import { formatCurrency } from '@/lib/utils'
import type { StatsDto } from '@/lib/types'
import { useStats } from '@/hooks/useStats'
import { TrendingUp, Layers, AlertCircle, RefreshCw } from 'lucide-react'

function StatItem({
  icon: Icon,
  label,
  value,
  color,
}: {
  icon: React.ElementType
  label: string
  value: string
  color: string
}) {
  return (
    <div className="flex items-center gap-4">
      <div className={`w-12 h-12 rounded-2xl flex items-center justify-center ${color} shrink-0`}>
        <Icon className="w-5 h-5" />
      </div>
      <div>
        <p className="text-2xl font-bold text-white">{value}</p>
        <p className="text-slate-400 text-sm">{label}</p>
      </div>
    </div>
  )
}

export function StatsBar() {
  const { stats, isLoading } = useStats()

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 animate-pulse">
        {[1, 2, 3].map(i => (
          <div key={i} className="h-20 rounded-2xl bg-slate-800/60" />
        ))}
      </div>
    )
  }

  if (!stats) return null

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <div className="rounded-2xl border border-slate-700/60 bg-slate-900/60 backdrop-blur-sm p-5">
        <StatItem
          icon={Layers}
          label="Active Airdrops"
          value={stats.totalActive.toString()}
          color="bg-brand-500/20 text-brand-400"
        />
      </div>

      <div className="rounded-2xl border border-slate-700/60 bg-slate-900/60 backdrop-blur-sm p-5">
        <StatItem
          icon={TrendingUp}
          label="Total Est. Value"
          value={formatCurrency(stats.totalEstimatedValue)}
          color="bg-accent-500/20 text-accent-400"
        />
      </div>

      <div className="rounded-2xl border border-rose-500/30 bg-slate-900/60 backdrop-blur-sm p-5">
        <StatItem
          icon={AlertCircle}
          label="Ending Today"
          value={stats.endingToday.toString()}
          color="bg-rose-500/20 text-rose-400"
        />
      </div>
    </div>
  )
}
