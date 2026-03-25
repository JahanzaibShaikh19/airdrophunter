import Cookies from 'js-cookie'

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'

export const getAuthToken = () => {
  return Cookies.get('pro_token')
}

export const setAuthToken = (token: string, expiresAt: number) => {
  Cookies.set('pro_token', token, { expires: new Date(expiresAt) })
}

export const fetcher = async (url: string) => {
  const token = getAuthToken()
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(`${API_URL}${url}`, { headers })

  if (!res.ok) {
    const errorBody = await res.json().catch(() => ({}))
    throw new Error(errorBody.error || `HTTP error! status: ${res.status}`)
  }

  return res.json()
}

// Custom specialized fetchers (for non-SWR POST calls)
export const checkWallet = async (address: string) => {
  const res = await fetch(`${API_URL}/wallet/check`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ address })
  })
  if (!res.ok) throw new Error('Failed to check wallet')
  return res.json()
}

export const activateProLicense = async (email: string, licenseKey: string) => {
  const res = await fetch(`${API_URL}/auth/activate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, licenseKey })
  })
  
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || 'Failed to activate license')
  }
  
  const data = await res.json()
  if (data.token) {
    setAuthToken(data.token, data.expiresAt)
  }
  return data
}
