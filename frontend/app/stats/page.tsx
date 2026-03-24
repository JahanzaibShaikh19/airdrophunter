'use client'

import { useStats } from '@/hooks/useStats'
import { formatCurrency } from '@/lib/utils'
import { TrendingUp, Layers, AlertCircle, BarChart2, RefreshCw } from 'lucide-react'

function StatCard({
  icon: Icon,
  label,
  value,
  subtitle,
  accent,
}: {
  icon: React.ElementType
  label: string
  value: string
  subtitle?: string
  accent: string
}) {
  return (
    <div className={`rounded-2xl border bg-slate-900/60 backdrop-blur-sm p-6 border-slate-700/60 hover:border-${accent}/40 transition-all duration-300 group`}>
      <div className={`w-12 h-12 rounded-2xl bg-${accent}/10 flex items-center justify-center mb-4 group-hover:bg-${accent}/20 transition-colors`}>
        <Icon className={`w-6 h-6 text-${accent}`} />
      </div>
      <p className="text-3xl font-extrabold text-white mb-1">{value}</p>
      <p className="text-white/80 font-semibold text-sm">{label}</p>
      {subtitle && <p className="text-slate-500 text-xs mt-1">{subtitle}</p>}
    </div>
  )
}

export default function StatsPage() {
  const { stats, isLoading, error } = useStats()

  return (
    <div className="space-y-8">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-brand-500/20 flex items-center justify-center">
          <BarChart2 className="w-5 h-5 text-brand-400" />
        </div>
        <div>
          <h1 className="text-2xl font-extrabold text-white">Stats Dashboard</h1>
          <p className="text-slate-400 text-sm flex items-center gap-1.5">
            <RefreshCw className="w-3 h-3" />
            Auto-refreshes every 30 seconds
          </p>
        </div>
      </div>

      {error && (
        <div className="rounded-2xl border border-rose-500/40 bg-rose-500/10 p-5 text-rose-300 text-sm">
          Failed to load stats. Is the backend running?
        </div>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5 animate-pulse">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-44 rounded-2xl bg-slate-800/60" />
          ))}
        </div>
      )}

      {stats && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
            <StatCard
              icon={Layers}
              label="Active Airdrops"
              value={stats.totalActive.toString()}
              subtitle="Currently claimable"
              accent="brand-400"
            />
            <StatCard
              icon={TrendingUp}
              label="Total Est. Value"
              value={formatCurrency(stats.totalEstimatedValue)}
              subtitle="Across all active airdrops"
              accent="accent-400"
            />
            <StatCard
              icon={AlertCircle}
              label="Ending Today"
              value={stats.endingToday.toString()}
              subtitle="Claim before midnight UTC"
              accent="rose-400"
            />
          </div>

          {/* Progress bar: ending today % */}
          {stats.totalActive > 0 && stats.endingToday > 0 && (
            <div className="rounded-2xl border border-slate-700/60 bg-slate-900/60 backdrop-blur-sm p-6">
              <p className="text-slate-300 font-semibold mb-3">Urgency Ratio</p>
              <div className="flex items-center gap-4">
                <div className="flex-1 h-3 rounded-full bg-slate-800 overflow-hidden">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-rose-500 to-orange-400 transition-all duration-700"
                    style={{ width: `${Math.round((stats.endingToday / stats.totalActive) * 100)}%` }}
                  />
                </div>
                <span className="text-rose-400 font-bold text-sm shrink-0">
                  {Math.round((stats.endingToday / stats.totalActive) * 100)}% ending today
                </span>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
