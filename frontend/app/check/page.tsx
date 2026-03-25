"use client"

import React, { useState } from "react"
import { motion } from "framer-motion"
import { Search, Loader2, Share2, CheckCircle2, ShieldAlert } from "lucide-react"
import { checkWallet } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"

export default function WalletChecker() {
  const [address, setAddress] = useState("")
  const [isSearching, setIsSearching] = useState(false)
  const [result, setResult] = useState<any>(null)
  const [error, setError] = useState<string | null>(null)

  const handleCheck = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!address) return

    setIsSearching(true)
    setError(null)
    setResult(null)

    try {
      const data = await checkWallet(address)
      setResult(data)
    } catch (err: any) {
      setError(err.message || "Failed to verify wallet eligibility.")
    } finally {
      setIsSearching(false)
    }
  }

  const copyToClipboard = () => {
    navigator.clipboard.writeText(`My wallet ${address.substring(0, 6)}... is eligible for ${result.eligibleProjects.length} airdrops! Total Est. Value: $${result.totalEstimatedValue} 🚀 via AirdropHunter.io`)
    alert("Copied to clipboard!")
  }

  // Simple animation variants
  const containerVars = {
    hidden: { opacity: 0, y: 30 },
    show: { opacity: 1, y: 0, transition: { staggerChildren: 0.1, duration: 0.5 } }
  }

  return (
    <div className="max-w-4xl mx-auto space-y-12 py-12">
      <div className="text-center space-y-4">
        <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight">
          Wallet <span className="text-gradient">Checker</span>
        </h1>
        <p className="text-lg text-slate-400">
          Enter an EVM address or ENS strictly to scan historical on-chain footprint across our DB.
        </p>
      </div>

      <Card className="bg-glass-card border-brand-500/30 overflow-hidden relative">
        <div className="absolute top-0 right-0 w-[300px] h-[300px] bg-brand-500/10 blur-[100px] rounded-full pointer-events-none" />
        <CardContent className="p-8">
          <form onSubmit={handleCheck} className="flex flex-col md:flex-row gap-4 relative z-10">
            <div className="flex-grow">
              <Input 
                placeholder="0x... or ENS domain" 
                className="h-14 text-lg bg-black/40 border-white/10"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
              />
            </div>
            <Button 
              type="submit" 
              variant="gradient" 
              className="h-14 px-8 text-lg"
              disabled={isSearching || !address}
            >
              {isSearching ? <Loader2 className="w-5 h-5 mr-2 animate-spin" /> : <Search className="w-5 h-5 mr-2" />}
              {isSearching ? "Scanning Hash..." : "Analyze Wallet"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {error && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-4 bg-rose-500/10 border border-rose-500/20 rounded-xl flex items-center gap-3 text-rose-400">
          <ShieldAlert className="w-5 h-5" /> {error}
        </motion.div>
      )}

      {/* Results View */}
      {result && (
        <motion.div variants={containerVars} initial="hidden" animate="show" className="space-y-8">
          
          <div className="text-center space-y-2">
             {result.isEligible ? (
                <>
                  <div className="inline-flex items-center justify-center p-4 bg-emerald-500/10 rounded-full mb-4">
                    <CheckCircle2 className="w-12 h-12 text-emerald-400" />
                  </div>
                  <h2 className="text-3xl font-bold text-white">Congratulations!</h2>
                  <p className="text-slate-400 text-lg">{result.message}</p>
                  
                  <div className="py-6">
                    <p className="text-5xl font-bold text-gradient">${result.totalEstimatedValue}+</p>
                    <p className="text-sm uppercase tracking-wider text-slate-500 mt-2 font-medium">Estimated Airdrop Value</p>
                  </div>
                </>
             ) : (
                <>
                  <div className="inline-flex items-center justify-center p-4 bg-slate-800/50 rounded-full mb-4">
                    <ShieldAlert className="w-12 h-12 text-slate-500" />
                  </div>
                  <h2 className="text-3xl font-bold text-white">No Allocations Found</h2>
                  <p className="text-slate-400 text-lg">{result.message}</p>
                </>
             )}
          </div>

          {result.eligibleProjects.length > 0 && (
            <motion.div className="space-y-4">
              <div className="flex justify-between items-center border-b border-white/10 pb-4">
                <h3 className="text-xl font-bold">Eligible Projects Networks ({result.eligibleProjects.length})</h3>
                <Button variant="outline" size="sm" onClick={copyToClipboard}>
                  <Share2 className="w-4 h-4 mr-2" /> Share Result
                </Button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
                {result.eligibleProjects.map((proj: string, i: number) => (
                  <motion.div 
                    key={proj}
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: i * 0.1 }}
                    className="p-4 rounded-xl bg-slate-800/40 border border-white/5 flex items-center justify-between"
                  >
                    <span className="font-semibold">{proj}</span>
                    <Badge variant="live">LIVE</Badge>
                  </motion.div>
                ))}
              </div>
            </motion.div>
          )}

        </motion.div>
      )}

    </div>
  )
}
