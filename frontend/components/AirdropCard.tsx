"use client"

import React, { useState, useEffect } from "react"
import { motion } from "framer-motion"
import { formatDistanceToNow, isPast } from "date-fns"
import { Badge } from "./ui/badge"
import { Card, CardHeader, CardContent, CardFooter } from "./ui/card"
import { Button } from "./ui/button"
import type { Airdrop } from "@/hooks/useAirdrops"
import { Lock, Timer, ExternalLink, CheckCircle } from "lucide-react"
import Link from "next/link"

interface AirdropCardProps {
  airdrop: Airdrop
  isLocked?: boolean
  index: number
}

export function AirdropCard({ airdrop, isLocked = false, index }: AirdropCardProps) {
  const [timeLeft, setTimeLeft] = useState<string>("TBA")
  
  // Real-time countdown timer tick
  useEffect(() => {
    if (!airdrop.deadline) return
    const targetDate = new Date(airdrop.deadline)
    
    // Initial compute
    const updateTick = () => {
      if (isPast(targetDate)) {
        setTimeLeft("Ended")
      } else {
        setTimeLeft(formatDistanceToNow(targetDate, { addSuffix: true }))
      }
    }
    
    updateTick()
    const timer = setInterval(updateTick, 60000) // Update minute-by-minute
    return () => clearInterval(timer)
  }, [airdrop.deadline])

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.05 }}
      className="relative group h-full"
    >
      <Card className="h-full flex flex-col overflow-hidden transition-all duration-500 hover:shadow-[0_0_30px_rgba(52,211,153,0.15)] hover:border-emerald-500/30">
        
        <CardHeader className="pb-4">
          <div className="flex justify-between items-start">
            <div className="flex gap-4 items-center">
              {airdrop.logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img 
                  src={airdrop.logoUrl} 
                  alt={airdrop.name} 
                  className="w-12 h-12 rounded-full border border-white/10 p-1 flex-shrink-0 object-contain bg-black/40"
                />
              ) : (
                <div className="w-12 h-12 rounded-full border border-white/10 flex items-center justify-center bg-slate-800 flex-shrink-0">
                  <span className="font-bold text-slate-300">{airdrop.symbol?.substring(0, 2) || '?'}</span>
                </div>
              )}
              
              <div>
                <h3 className="font-bold text-xl text-white tracking-tight">{airdrop.name}</h3>
                <span className="text-sm font-medium text-slate-400">{airdrop.symbol}</span>
              </div>
            </div>
            
            <div className="flex flex-col items-end gap-2">
              <Badge 
                variant={airdrop.status === 'LIVE' ? 'live' : airdrop.status === 'SOON' ? 'soon' : 'ended'}
              >
                {airdrop.status}
              </Badge>
              {airdrop.isHot && <Badge variant="hot">HOT 🔥</Badge>}
            </div>
          </div>
        </CardHeader>

        <CardContent className="flex-grow space-y-6 relative">
          
          <div className="p-4 rounded-xl bg-slate-900/50 border border-white/5">
            <p className="text-xs text-slate-400 font-medium uppercase tracking-wider mb-1">Est. Value per Wallet</p>
            <p className="text-2xl font-bold text-gradient">
              ${airdrop.estimatedValueMin} - ${airdrop.estimatedValueMax}
            </p>
          </div>

          <div className="space-y-3">
            <p className="text-sm text-slate-400 font-medium tracking-wider uppercase flex items-center gap-2">
              <CheckCircle className="w-4 h-4 text-emerald-500" /> Interaction Steps
            </p>
            <ul className="space-y-2">
              {airdrop.steps.slice(0, 3).map((step, i) => (
                <li key={i} className="text-sm text-slate-300 flex items-start flex-nowrap break-words">
                  <span className="text-emerald-500 mr-2 shrink-0">{i+1}.</span> 
                  <span className="line-clamp-2 leading-relaxed">{step}</span>
                </li>
              ))}
              {airdrop.steps.length > 3 && (
                <li className="text-xs text-slate-500 italic mt-1">+ {airdrop.steps.length - 3} more steps...</li>
              )}
            </ul>
          </div>
          
        </CardContent>

        <CardFooter className="border-t border-white/5 flex gap-4 pt-6 text-sm text-slate-400 justify-between items-center">
          <div className="flex items-center gap-1.5">
            <Timer className="w-4 h-4" />
            <span className={isPast(new Date(airdrop.deadline || 0)) ? 'text-rose-400' : ''}>
              {timeLeft}
            </span>
          </div>
          <div className="flex items-center gap-2">
             <Badge variant="outline" className="text-slate-500 border-white/10">{airdrop.category}</Badge>
          </div>
        </CardFooter>

        {/* PRO Overlay Filter Layer */}
        {isLocked && (
          <div className="absolute inset-0 z-20 backdrop-blur-md bg-[#050B14]/60 flex flex-col items-center justify-center p-6 border border-brand-500/20 rounded-xl transition-all duration-500">
            <Lock className="w-12 h-12 text-slate-400 mb-4 opacity-80" />
            <h4 className="text-xl font-bold text-white mb-2 text-center">PRO Action Required</h4>
            <p className="text-sm text-slate-400 text-center mb-6">Unlock full guides, unlimited airdrops, and real-time alerts.</p>
            <Link href="/pro">
              <Button variant="gradient" className="shadow-[0_0_20px_rgba(52,211,153,0.3)]">
                Upgrade to PRO
              </Button>
            </Link>
          </div>
        )}
      </Card>
      
      {/* Absolute floating Link to block clicks behind overlay if not locked */}
      {!isLocked && (
         <a href={`https://airdrophunter.io/${airdrop.llamaSlug || ''}`} target="_blank" rel="noopener noreferrer" className="absolute top-4 right-4 z-10 w-8 h-8 flex items-center justify-center rounded-full bg-black/50 hover:bg-black/80 text-white/50 hover:text-white transition-colors opacity-0 group-hover:opacity-100 backdrop-blur-md transform translate-y-2 group-hover:translate-y-0 duration-300">
            <ExternalLink className="w-4 h-4" />
         </a>
      )}
    </motion.div>
  )
}
