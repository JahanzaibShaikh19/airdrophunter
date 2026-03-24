'use client'

import { useState } from 'react'
import { cn, formatCurrency } from '@/lib/utils'
import type { WalletCheckResponse } from '@/lib/types'
import { Search, CheckCircle, XCircle, Loader2, Wallet } from 'lucide-react'

const API = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

export function WalletChecker() {
  const [address, setAddress] = useState('')
  const [result, setResult] = useState<WalletCheckResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleCheck(e: React.FormEvent) {
    e.preventDefault()
    if (!address.trim()) return

    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const res = await fetch(`${API}/api/wallet/check`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ address: address.trim() }),
      })
      if (!res.ok) throw new Error(`Server error: ${res.status}`)
      const data: WalletCheckResponse = await res.json()
      setResult(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to check wallet')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Input Form */}
      <div className="rounded-2xl border border-slate-700/60 bg-slate-900/60 backdrop-blur-sm p-6">
        <div className="flex items-center gap-3 mb-5">
          <div className="w-10 h-10 rounded-xl bg-brand-500/20 flex items-center justify-center">
            <Wallet className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <h2 className="text-white font-bold text-lg">Wallet Eligibility Check</h2>
            <p className="text-slate-400 text-sm">Enter an EVM or Solana address</p>
          </div>
        </div>

        <form onSubmit={handleCheck} className="flex gap-3">
          <input
            type="text"
            value={address}
            onChange={e => setAddress(e.target.value)}
            placeholder="0x... or Solana address"
            className={cn(
              'flex-1 px-4 py-3 rounded-xl text-sm text-white placeholder-slate-500',
              'bg-slate-800/80 border border-slate-700/60',
              'focus:outline-none focus:border-brand-500/60 focus:ring-1 focus:ring-brand-500/30',
              'transition-all duration-200'
            )}
          />
          <button
            type="submit"
            disabled={loading || !address.trim()}
            className={cn(
              'px-5 py-3 rounded-xl font-semibold text-sm flex items-center gap-2',
              'bg-brand-600 hover:bg-brand-500 text-white',
              'disabled:opacity-50 disabled:cursor-not-allowed',
              'transition-all duration-200 active:scale-95'
            )}
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
            {loading ? 'Checking…' : 'Check'}
          </button>
        </form>
      </div>

      {/* Error */}
      {error && (
        <div className="rounded-2xl border border-rose-500/40 bg-rose-500/10 p-5 text-rose-300 text-sm">
          {error}
        </div>
      )}

      {/* Result */}
      {result && (
        <div className={cn(
          'rounded-2xl border p-6 animate-slide-up',
          result.eligible
            ? 'border-brand-500/40 bg-brand-500/5'
            : 'border-rose-500/40 bg-rose-500/5'
        )}>
          <div className="flex items-start gap-4">
            {result.eligible
              ? <CheckCircle className="w-7 h-7 text-brand-400 mt-0.5 shrink-0" />
              : <XCircle className="w-7 h-7 text-rose-400 mt-0.5 shrink-0" />
            }
            <div className="flex-1">
              <h3 className={cn(
                'text-lg font-bold mb-1',
                result.eligible ? 'text-brand-400' : 'text-rose-400'
              )}>
                {result.eligible ? '🎉 Eligible!' : 'Not Eligible'}
              </h3>
              <p className="text-slate-300 text-sm mb-4">{result.reason}</p>

              {result.eligible && (
                <>
                  <div className="flex items-center justify-between p-4 rounded-xl bg-slate-800/60 mb-4">
                    <span className="text-slate-400 text-sm">Estimated Total Reward</span>
                    <span className="text-2xl font-bold text-white">
                      {formatCurrency(result.estimatedReward)}
                    </span>
                  </div>
                  {result.eligibleAirdrops.length > 0 && (
                    <div>
                      <p className="text-slate-400 text-xs mb-2 uppercase tracking-wider">Eligible airdrops</p>
                      <div className="flex flex-wrap gap-2">
                        {result.eligibleAirdrops.map(name => (
                          <span
                            key={name}
                            className="px-3 py-1 rounded-full text-xs bg-brand-500/20 text-brand-300 border border-brand-500/30"
                          >
                            {name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
