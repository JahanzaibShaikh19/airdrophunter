import type { Metadata } from "next"
import { Inter } from "next/font/google"
import "./globals.css"
import { Navbar } from "@/components/Navbar"

const inter = Inter({ subsets: ["latin"] })

export const metadata: Metadata = {
  title: "AirdropHunter Dashboard",
  description: "Monitor the DeFi ecosystem for lucrative airdrops in real-time.",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" className="dark">
      <body className={`${inter.className} min-h-screen relative overflow-x-hidden`}>
        {/* Deep ambient background gradients */}
        <div className="fixed inset-0 -z-10 bg-[#050B14]">
          <div className="absolute top-0 left-1/4 w-[500px] h-[500px] bg-emerald-500/10 rounded-full blur-[120px] mix-blend-screen pointer-events-none" />
          <div className="absolute bottom-1/4 right-1/4 w-[600px] h-[600px] bg-cyan-500/10 rounded-full blur-[150px] mix-blend-screen pointer-events-none" />
        </div>
        
        <Navbar />
        
        <main className="container mx-auto px-4 py-8 relative z-10 w-full">
          {children}
        </main>
      </body>
    </html>
  )
}
