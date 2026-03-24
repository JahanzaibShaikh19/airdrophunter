import type { Metadata } from 'next'
import { HotList } from '@/components/HotList'
import { Flame } from 'lucide-react'

export const metadata: Metadata = {
  title: 'Hot Airdrops — AirdropHunter',
  description: 'Top crypto airdrops ranked by estimated value. Claim the most lucrative token drops.',
}

export default function HotPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-orange-500/20 flex items-center justify-center">
          <Flame className="w-5 h-5 text-orange-400 fill-orange-400" />
        </div>
        <div>
          <h1 className="text-2xl font-extrabold text-white">Hot Airdrops 🔥</h1>
          <p className="text-slate-400 text-sm">Ranked by highest estimated value</p>
        </div>
      </div>
      <HotList />
    </div>
  )
}
