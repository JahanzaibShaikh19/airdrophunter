'use client'

import { cn, formatCurrency, formatCountdown, isEndingSoon } from '@/lib/utils'
import type { Airdrop } from '@/lib/types'
import { ExternalLink, Clock, Zap } from 'lucide-react'

interface Props {
  airdrop: Airdrop
  rank?: number
}

const CHAIN_COLORS: Record<string, string> = {
  'Ethereum':    'bg-blue-500/20 text-blue-300 border-blue-500/30',
  'Ethereum L2': 'bg-purple-500/20 text-purple-300 border-purple-500/30',
  'Multi-chain': 'bg-amber-500/20 text-amber-300 border-amber-500/30',
  'Cosmos':      'bg-rose-500/20 text-rose-300 border-rose-500/30',
  'Hyperliquid': 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30',
}

export function AirdropCard({ airdrop, rank }: Props) {
  const urgency = isEndingSoon(airdrop.endsAt)
  const chainColor = CHAIN_COLORS[airdrop.chain] ?? 'bg-slate-500/20 text-slate-300 border-slate-500/30'

  return (
    <article className={cn(
      'group relative rounded-2xl border bg-slate-900/60 backdrop-blur-sm p-5',
      'transition-all duration-300 hover:scale-[1.02] hover:shadow-2xl hover:shadow-brand-500/10',
      urgency
        ? 'border-rose-500/40 shadow-rose-500/5 shadow-lg'
        : 'border-slate-700/60 hover:border-brand-500/40',
      'animate-fade-in'
    )}>
      {rank && (
        <div className="absolute -top-3 -left-3 w-8 h-8 rounded-full bg-gradient-to-br from-brand-400 to-accent-500 flex items-center justify-center text-xs font-bold text-white shadow-lg">
          #{rank}
        </div>
      )}

      {urgency && (
        <div className="absolute top-4 right-4 flex items-center gap-1 text-rose-400 text-xs font-semibold">
          <Zap className="w-3 h-3 fill-rose-400" />
          Ending soon
        </div>
      )}

      <div className="flex items-start justify-between mb-3 mt-1">
        <div>
          <h3 className="font-bold text-white text-lg leading-tight group-hover:text-brand-400 transition-colors">
            {airdrop.name}
          </h3>
          <p className="text-slate-400 text-sm mt-0.5">{airdrop.protocol}</p>
        </div>
        <span className="ml-3 shrink-0 px-2.5 py-1 rounded-full text-xs font-bold bg-brand-500/20 text-brand-300 border border-brand-500/30">
          {airdrop.token}
        </span>
      </div>

      <p className="text-slate-400 text-sm leading-relaxed line-clamp-2 mb-4">
        {airdrop.description}
      </p>

      <div className="flex items-center justify-between">
        <div>
          <p className="text-2xl font-bold text-white">
            {formatCurrency(airdrop.estimatedValue)}
          </p>
          <p className="text-slate-500 text-xs mt-0.5">Est. value</p>
        </div>

        <div className="text-right">
          <span className={cn('inline-block px-2 py-0.5 rounded-full text-xs border mb-1', chainColor)}>
            {airdrop.chain}
          </span>
          <div className={cn(
            'flex items-center gap-1 text-xs',
            urgency ? 'text-rose-400' : 'text-slate-400'
          )}>
            <Clock className="w-3 h-3" />
            {formatCountdown(airdrop.endsAt)}
          </div>
        </div>
      </div>

      {airdrop.websiteUrl && (
        <a
          href={airdrop.websiteUrl}
          target="_blank"
          rel="noopener noreferrer"
          className={cn(
            'mt-4 flex items-center justify-center gap-2 w-full py-2.5 rounded-xl text-sm font-semibold',
            'bg-brand-600/20 text-brand-300 border border-brand-500/30',
            'hover:bg-brand-500 hover:text-white hover:border-brand-500 transition-all duration-200'
          )}
        >
          Claim Airdrop
          <ExternalLink className="w-3.5 h-3.5" />
        </a>
      )}
    </article>
  )
}
