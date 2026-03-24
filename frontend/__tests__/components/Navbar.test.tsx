import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'

// Mock next/navigation before importing Navbar
jest.mock('next/navigation', () => ({
  usePathname: jest.fn(),
}))

// Mock next/link to render a plain <a>
jest.mock('next/link', () => {
  const MockLink = ({ children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  )
  MockLink.displayName = 'MockLink'
  return MockLink
})

import { usePathname } from 'next/navigation'
import { Navbar } from '@/components/Navbar'

const mockUsePathname = usePathname as jest.MockedFunction<typeof usePathname>

describe('Navbar', () => {
  beforeEach(() => {
    mockUsePathname.mockReturnValue('/')
  })

  it('renders the brand logo text', () => {
    render(<Navbar />)
    expect(screen.getByText('AirdropHunter')).toBeInTheDocument()
  })

  it('renders all four navigation links on desktop', () => {
    render(<Navbar />)
    expect(screen.getByText('All Airdrops')).toBeInTheDocument()
    // Hot / Wallet Check / Stats — use getAllByText since mobile also renders them
    expect(screen.getAllByText(/Hot/i).length).toBeGreaterThanOrEqual(1)
    expect(screen.getAllByText(/Wallet Check/i).length).toBeGreaterThanOrEqual(1)
    expect(screen.getAllByText(/Stats/i).length).toBeGreaterThanOrEqual(1)
  })

  it('logo links to home /', () => {
    render(<Navbar />)
    const homeLinks = screen.getAllByRole('link').filter(l => l.getAttribute('href') === '/')
    expect(homeLinks.length).toBeGreaterThan(0)
  })

  it('highlights active route link', () => {
    mockUsePathname.mockReturnValue('/hot')
    render(<Navbar />)
    // The Hot link should have the active class in its class attribute
    const hotLinks = screen.getAllByText(/Hot/i)
    const activeLink = hotLinks.find(el =>
      el.closest('a')?.className.includes('brand-400')
    )
    expect(activeLink).toBeDefined()
  })

  it('toggles mobile menu on hamburger click', () => {
    render(<Navbar />)
    const menuButton = screen.getByRole('button', { name: /Toggle menu/i })

    // Initially the mobile nav is not shown (no duplicate "All Airdrops")
    expect(screen.getAllByText('All Airdrops')).toHaveLength(1)

    fireEvent.click(menuButton)
    // After click, mobile nav shows a second instance
    expect(screen.getAllByText('All Airdrops')).toHaveLength(2)

    fireEvent.click(menuButton)
    // Clicking again closes it
    expect(screen.getAllByText('All Airdrops')).toHaveLength(1)
  })
})
