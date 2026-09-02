const baseUrl = import.meta.env.VITE_API_URL ?? '/api'

export class ApiError extends Error { constructor(public status: number, message: string) { super(message) } }

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('smartwatch_token')
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new ApiError(response.status, body?.message ?? body?.error ?? 'Something went wrong. Please try again.')
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
