"use client"

import React, { useState } from "react"
import { useAirdrops, Airdrop } from "@/hooks/useAirdrops"
import { useStats } from "@/hooks/useStats"
import { getAuthToken } from "@/lib/api"
import { AirdropCard } from "@/components/AirdropCard"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent } from "@/components/ui/card"
import { Rocket, Target, Zap } from "lucide-react"

export default function Dashboard() {
  const { airdrops, isLoading } = useAirdrops()
  const { stats } = useStats()
  const [activeTab, setActiveTab] = useState<string>("all")
  
  const isPro = !!getAuthToken()

  // Filter logic
  const filteredAirdrops = React.useMemo(() => {
    if (!airdrops) return []
    let filtered = [...airdrops]
    
    switch (activeTab) {
      case "live":
        filtered = filtered.filter(a => a.status === 'LIVE')
        break
      case "soon":
        filtered = filtered.filter(a => a.status === 'SOON')
        break
      case "defi":
        filtered = filtered.filter(a => a.category === 'DEFI')
        break
      case "l2":
        filtered = filtered.filter(a => a.category === 'L2')
        break
      case "ai":
        filtered = filtered.filter(a => a.category === 'AI')
        break
    }
    
    // Default sorting: Hot first, then by max estimated value
    return filtered.sort((a, b) => {
      if (a.isHot && !b.isHot) return -1
      if (!a.isHot && b.isHot) return 1
      return b.estimatedValueMax - a.estimatedValueMax
    })
  }, [airdrops, activeTab])

  return (
    <div className="space-y-10 pb-20">
      
      {/* Platform Hero / Stats Bar */}
      <section className="space-y-6">
        <div className="text-center space-y-4 max-w-2xl mx-auto py-8">
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight">
            Hunt the Most <span className="text-gradient">Lucrative</span> Airdrops
          </h1>
          <p className="text-lg text-slate-400">
            Real-time monitoring of the brightest protocols in DeFi. We track the criteria, you reap the rewards.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card className="bg-glass-card border-brand-500/20">
            <CardContent className="p-6 flex items-center space-x-4">
              <div className="p-3 bg-brand-500/10 rounded-xl">
                <Target className="w-8 h-8 text-brand-400" />
              </div>
              <div>
                <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Active Airdrops</p>
                <h3 className="text-3xl font-bold font-mono">
                  {stats?.totalActiveAirdrops || "0"}
                </h3>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-glass-card border-cyan-500/20">
            <CardContent className="p-6 flex items-center space-x-4">
              <div className="p-3 bg-cyan-500/10 rounded-xl">
                <Zap className="w-8 h-8 text-cyan-400" />
              </div>
              <div>
                <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Total Value Tracked</p>
                <h3 className="text-3xl font-bold font-mono text-cyan-400">
                  ${(stats?.totalEstimatedValueMax || 0).toLocaleString()}
                </h3>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-glass-card border-rose-500/20">
            <CardContent className="p-6 flex items-center space-x-4">
              <div className="p-3 bg-rose-500/10 rounded-xl">
                <Rocket className="w-8 h-8 text-rose-400" />
              </div>
              <div>
                <p className="text-sm font-medium text-slate-400 uppercase tracking-wider">Ending Today</p>
                <h3 className="text-3xl font-bold font-mono text-rose-400">
                  {stats?.endingTodayCount || "0"}
                </h3>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Main Board */}
      <section className="space-y-6">
        <div className="flex flex-col md:flex-row justify-between items-center gap-4">
          <h2 className="text-3xl font-bold tracking-tight">Intelligence Board</h2>
          
          <Tabs defaultValue="all" onValueChange={setActiveTab} className="w-full md:w-auto">
            <TabsList className="bg-glass border border-white/10 w-full md:w-auto overflow-x-auto justify-start">
              <TabsTrigger value="all">All Targets</TabsTrigger>
              <TabsTrigger value="live">LIVE</TabsTrigger>
              <TabsTrigger value="soon">SOON</TabsTrigger>
              <TabsTrigger value="defi">DeFi</TabsTrigger>
              <TabsTrigger value="l2">Layer 2</TabsTrigger>
              <TabsTrigger value="ai">AI</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
             {[...Array(6)].map((_, i) => (
                <div key={i} className="h-[400px] rounded-xl bg-slate-800/50 animate-pulse border border-white/5" />
             ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
            {filteredAirdrops.map((airdrop, i) => {
              // The backend limits free users to exactly 4 results. 
              // Any less than the expected volume organically, or exactly 4, signifies gating.
              // We'll simulate locking dynamically: If we get 4 results and user is not pro, 
              // we can render a beautiful "locked" dummy card as the 5th to encourage upgrades.
              return (
                <AirdropCard key={airdrop.id} airdrop={airdrop} index={i} />
              )
            })}
            
            {/* Soft Client Lock: If not PRO and exactly 4 records returned, render the Upsell Card */}
            {!isPro && filteredAirdrops.length === 4 && activeTab === "all" && (
                <AirdropCard 
                  key="dummy-pro-lock"
                  index={4}
                  isLocked={true}
                  airdrop={{
                    id: -1, name: "Premium Airdrop", symbol: "PRO", estimatedValueMin: 5000, 
                    estimatedValueMax: 15000, status: "LIVE", category: "OTHER", 
                    steps: ["Access high-value restricted airdrops"], isHot: true, isPro: true
                  }} 
                />
            )}
          </div>
        )}
      </section>

    </div>
  )
}
