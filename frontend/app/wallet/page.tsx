import type { Metadata } from 'next'
import { WalletChecker } from '@/components/WalletChecker'
import { ShieldCheck } from 'lucide-react'

export const metadata: Metadata = {
  title: 'Wallet Eligibility Check — AirdropHunter',
  description: 'Check if your Ethereum or Solana wallet is eligible for active crypto airdrops.',
}

export default function WalletPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-accent-500/20 flex items-center justify-center">
          <ShieldCheck className="w-5 h-5 text-accent-400" />
        </div>
        <div>
          <h1 className="text-2xl font-extrabold text-white">Wallet Check</h1>
          <p className="text-slate-400 text-sm">Verify your eligibility across all active airdrops</p>
        </div>
      </div>
      <WalletChecker />
    </div>
  )
}
