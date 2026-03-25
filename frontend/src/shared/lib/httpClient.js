import { API_BASE_URL } from '../config/env'

export async function requestApi({ method, path, token, query, body }) {
  const url = new URL(`${API_BASE_URL}${path}`)

  if (query && typeof query === 'object') {
    Object.entries(query).forEach(([key, value]) => {
      if (value !== '' && value !== null && value !== undefined) {
        url.searchParams.set(key, String(value))
      }
    })
  }

  const headers = {
    Accept: 'application/json',
  }

  if (body !== undefined && body !== null) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url.toString(), {
    method,
    headers,
    body: body !== undefined && body !== null ? JSON.stringify(body) : undefined,
  })

  const text = await response.text()
  let data = text

  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = text
  }

  return {
    status: response.status,
    ok: response.ok,
    data,
  }
}
