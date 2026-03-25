"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Shield, Search, Zap } from "lucide-react"
import { Button } from "./ui/button"
import { cn } from "@/lib/utils"

export function Navbar() {
  const pathname = usePathname()

  const navItems = [
    { name: "Dashboard", href: "/", icon: <Zap className="w-4 h-4 mr-2" /> },
    { name: "Wallet Checker", href: "/check", icon: <Search className="w-4 h-4 mr-2" /> },
  ]

  return (
    <header className="sticky top-0 z-50 w-full border-b border-white/10 bg-background/60 backdrop-blur-xl">
      <div className="container mx-auto flex h-16 items-center justify-between px-4">
        
        {/* Logo */}
        <div className="flex items-center gap-2">
          <div className="bg-brand-500 rounded-lg p-1.5 shadow-[0_0_15px_rgba(34,197,94,0.4)]">
            <Shield className="w-5 h-5 text-white" />
          </div>
          <Link href="/" className="flex items-center space-x-2">
            <span className="font-bold text-xl tracking-tight text-white hover:text-brand-400 transition-colors">
              Airdrop<span className="text-brand-400">Hunter</span>
            </span>
          </Link>
        </div>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center space-x-6">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center text-sm font-medium transition-colors hover:text-brand-400",
                pathname === item.href ? "text-brand-400" : "text-muted-foreground"
              )}
            >
              {item.icon}
              {item.name}
            </Link>
          ))}
        </nav>

        {/* CTA */}
        <div className="flex items-center space-x-4">
          <Link href="/pro">
            <Button variant="gradient" size="sm" className="hidden sm:flex">
              Unlock PRO
            </Button>
          </Link>
        </div>
      </div>
    </header>
  )
}
