"use client"

import React, { useState, useEffect } from "react"
import { motion } from "framer-motion"
import { Check, X, Shield, Key, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { activateProLicense, getAuthToken } from "@/lib/api"
import { useRouter } from "next/navigation"

export default function ProUpgrade() {
  const router = useRouter()
  const [email, setEmail] = useState("")
  const [licenseKey, setLicenseKey] = useState("")
  const [isActivating, setIsActivating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  const isAlreadyPro = !!getAuthToken()

  useEffect(() => {
    if (isAlreadyPro) {
      router.push("/")
    }
  }, [isAlreadyPro, router])

  const handleActivate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email || !licenseKey) return
    
    setIsActivating(true)
    setError(null)
    
    try {
      await activateProLicense(email, licenseKey)
      // Redirect to dash once token is injected
      router.push("/")
    } catch (err: any) {
      setError(err.message || "Activation failed. Please check your credentials.")
    } finally {
      setIsActivating(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto py-12 px-4 space-y-16">
      <div className="text-center space-y-6 max-w-3xl mx-auto">
         <Badge variant="live" className="mb-4">LIMITED TIME OFFER</Badge>
         <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight">
           Supercharge Your <span className="text-gradient">Airdrop Alpha</span>
         </h1>
         <p className="text-lg text-slate-400">
           Gain uncapped access to our entire on-chain intelligence database. Receive real-time Telegram alerts before the masses catch on.
         </p>
      </div>

      <div className="grid md:grid-cols-2 gap-8 items-start">
        
        {/* Comparison Column */}
        <div className="space-y-6">
          <Card className="bg-glass-card border-white/5">
            <CardHeader className="border-b border-white/5 pb-4">
               <h3 className="text-xl font-bold text-white">AirdropHunter Base</h3>
            </CardHeader>
            <CardContent className="pt-6 space-y-4">
              <Feature text="Top 4 trending airdrops" included={true} />
              <Feature text="Basic protocol filters" included={true} />
              <Feature text="Unlimited Wallet Checks" included={true} />
              <Feature text="Complete Airdrop Database Access" included={false} />
              <Feature text="Real-time Telegram Alerts" included={false} />
              <Feature text="API & Webhook programmatic access" included={false} />
            </CardContent>
          </Card>
          
          <Card className="bg-glass-card border-brand-500/30 relative overflow-hidden">
             <div className="absolute inset-0 bg-gradient-to-br from-emerald-500/10 to-transparent pointer-events-none" />
            <CardHeader className="border-b border-white/10 pb-4 relative z-10 flex flex-row justify-between items-center">
               <div className="flex items-center gap-2">
                 <Shield className="w-5 h-5 text-emerald-400" />
                 <h3 className="text-xl font-bold text-emerald-400">PRO Membership</h3>
               </div>
               <span className="text-2xl font-bold text-white">$9<span className="text-sm text-slate-400 font-normal">/lifetime</span></span>
            </CardHeader>
            <CardContent className="pt-6 space-y-4 relative z-10">
              <Feature text="Unlimited Airdrop Database Access" included={true} />
              <Feature text="Uncapped Live Filters" included={true} />
              <Feature text="Unlimited Wallet Checks" included={true} />
              <Feature text="Real-time Telegram Alerts (@AirdropHunterBot)" included={true} highlight />
              <Feature text="API & Webhook programmatic access" included={true} />
              
              <div className="pt-6">
                 {/* Re-directs to a Gumroad checkout URL; for demo linking directly # */}
                 <a href="https://gumroad.com" target="_blank" rel="noopener noreferrer" className="w-full flex">
                    <Button variant="gradient" className="w-full h-12 text-lg">
                      Purchase PRO on Gumroad
                    </Button>
                 </a>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Activation Column */}
        <div className="relative">
           <div className="absolute top-0 right-0 w-[400px] h-[400px] bg-cyan-500/10 blur-[100px] rounded-full pointer-events-none -z-10" />
           <Card className="bg-glass-card border-white/10 p-8 shadow-2xl">
             <div className="mb-8">
                <div className="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center mb-4 border border-white/10">
                  <Key className="w-6 h-6 text-slate-300" />
                </div>
                <h2 className="text-2xl font-bold text-white tracking-tight">Activate License</h2>
                <p className="text-slate-400 mt-2">Already purchased via Gumroad? Enter your email and license key to unlock your browser session.</p>
             </div>
             
             <form onSubmit={handleActivate} className="space-y-5">
                <div className="space-y-2">
                  <label className="text-sm font-medium text-slate-300">Gumroad Email</label>
                  <Input 
                    type="email" 
                    placeholder="user@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    className="h-12 bg-black/40"
                  />
                </div>
                
                <div className="space-y-2">
                  <label className="text-sm font-medium text-slate-300">License Key</label>
                  <Input 
                    type="text" 
                    placeholder="XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX"
                    value={licenseKey}
                    onChange={(e) => setLicenseKey(e.target.value)}
                    required
                    className="h-12 bg-black/40 font-mono text-sm"
                  />
                </div>
                
                {error && <p className="text-sm text-rose-400 bg-rose-500/10 p-3 rounded-lg border border-rose-500/20">{error}</p>}

                <Button 
                   type="submit" 
                   disabled={isActivating || !email || !licenseKey}
                   className="w-full h-12 bg-white text-black hover:bg-slate-200"
                >
                  {isActivating ? <Loader2 className="w-5 h-5 mr-2 animate-spin" /> : null}
                  {isActivating ? "Verifying..." : "Activate Session"}
                </Button>
             </form>
           </Card>
        </div>

      </div>
    </div>
  )
}

function Feature({ text, included, highlight = false }: { text: string, included: boolean, highlight?: boolean }) {
  return (
    <div className={`flex items-start gap-3 ${highlight ? 'text-emerald-400 font-medium' : (included ? 'text-slate-200' : 'text-slate-500')}`}>
      {included ? (
        <Check className={`w-5 h-5 shrink-0 ${highlight ? 'text-emerald-400' : 'text-slate-400'}`} />
      ) : (
        <X className="w-5 h-5 shrink-0 text-slate-600" />
      )}
      <span className="leading-tight">{text}</span>
    </div>
  )
}

// Ensure the Badge primitive exists strictly for the mock layout styling
function Badge({ children, className, variant = "default" }: any) {
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${variant === 'live' ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400 animate-pulse-slow' : 'bg-slate-800 text-slate-300 border-white/10'} ${className}`}>
      {children}
    </span>
  )
}
