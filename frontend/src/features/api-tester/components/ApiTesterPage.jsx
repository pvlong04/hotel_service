import { useState } from 'react'
import { API_BASE_URL } from '../../../shared/config/env'
import { apiModules } from '../../../entities/api/apiModules'
import { EndpointCard } from './EndpointCard'

const TOKEN_KEY = 'hotel_service_api_token'

function getInitialToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function ApiTesterPage() {
  const [token, setToken] = useState(getInitialToken)

  const onTokenChange = (value) => {
    setToken(value)
    localStorage.setItem(TOKEN_KEY, value)
  }

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-6 text-slate-900">
      <div className="mx-auto max-w-7xl space-y-5">
        <header className="rounded-xl bg-white p-5 shadow-sm">
          <h1 className="text-2xl font-bold">Hotel Service API Tester</h1>
          <p className="mt-1 text-sm text-slate-600">Frontend ReactJS + Vite + Tailwind để test nhanh các API backend.</p>
          <p className="mt-1 text-xs text-slate-500">
            Base URL: <span className="font-mono">{API_BASE_URL}</span>
          </p>

          <label className="mt-4 block text-sm font-medium text-slate-700">
            Bearer token dùng cho endpoint cần auth
            <textarea
              rows={3}
              value={token}
              onChange={(event) => onTokenChange(event.target.value)}
              className="mt-1 w-full rounded-md border border-slate-300 p-2 font-mono text-xs"
              placeholder="Dán accessToken vào đây"
            />
          </label>
        </header>

        {apiModules.map((moduleItem) => (
          <section key={moduleItem.name} className="space-y-3">
            <h2 className="text-lg font-semibold text-slate-800">{moduleItem.name}</h2>
            <div className="grid gap-3 xl:grid-cols-2">
              {moduleItem.endpoints.map((endpoint) => (
                <EndpointCard key={`${moduleItem.name}-${endpoint.name}`} endpoint={endpoint} token={token} />
              ))}
            </div>
          </section>
        ))}
      </div>
    </main>
  )
}
