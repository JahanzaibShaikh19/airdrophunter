import { formatCurrency, formatCountdown, isEndingSoon, cn } from '@/lib/utils'

describe('cn()', () => {
  it('merges class strings', () => {
    expect(cn('a', 'b')).toBe('a b')
  })

  it('removes falsy values', () => {
    expect(cn('a', false && 'b', undefined, 'c')).toBe('a c')
  })

  it('resolves Tailwind conflicts (last wins)', () => {
    // tailwind-merge: p-2 overrides p-4 when both present
    const result = cn('p-4', 'p-2')
    expect(result).toBe('p-2')
  })
})

describe('formatCurrency()', () => {
  it('formats whole numbers with USD symbol', () => {
    expect(formatCurrency(2500)).toMatch(/\$2,500/)
  })

  it('formats zero as $0', () => {
    expect(formatCurrency(0)).toMatch(/\$0/)
  })

  it('formats large values with commas', () => {
    expect(formatCurrency(1_000_000)).toMatch(/\$1,000,000/)
  })

  it('rounds fractional values (no decimal places)', () => {
    const result = formatCurrency(99.99)
    expect(result).not.toContain('.')
  })
})

describe('formatCountdown()', () => {
  it('returns "Ended" for past dates', () => {
    const past = new Date(Date.now() - 10_000).toISOString()
    expect(formatCountdown(past)).toBe('Ended')
  })

  it('returns days+hours for dates > 1 day away', () => {
    const future = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000 + 3 * 60 * 60 * 1000).toISOString()
    const result = formatCountdown(future)
    expect(result).toMatch(/2d \dh left/)
  })

  it('returns hours+minutes for dates < 1 day away', () => {
    const future = new Date(Date.now() + 3 * 60 * 60 * 1000 + 30 * 60 * 1000).toISOString()
    const result = formatCountdown(future)
    expect(result).toMatch(/3h \d+m left/)
  })
})

describe('isEndingSoon()', () => {
  it('returns true when airdrop ends in less than 3 days', () => {
    const soon = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString()
    expect(isEndingSoon(soon)).toBe(true)
  })

  it('returns false when airdrop ends in more than 3 days', () => {
    const later = new Date(Date.now() + 10 * 24 * 60 * 60 * 1000).toISOString()
    expect(isEndingSoon(later)).toBe(false)
  })

  it('returns true for dates in the past', () => {
    const past = new Date(Date.now() - 1000).toISOString()
    expect(isEndingSoon(past)).toBe(true)
  })

  it('returns false for exactly 3 days away', () => {
    const exact = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000 + 60_000).toISOString()
    expect(isEndingSoon(exact)).toBe(false)
  })
})
