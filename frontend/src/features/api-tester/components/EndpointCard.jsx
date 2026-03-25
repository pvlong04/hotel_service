import { useApiTester } from '../hooks/useApiTester'
import { JsonBlock } from '../../../shared/ui/JsonBlock'

export function EndpointCard({ endpoint, token }) {
  const {
    params,
    pathValues,
    queryRaw,
    bodyRaw,
    response,
    loading,
    error,
    setQueryRaw,
    setBodyRaw,
    onPathChange,
    execute,
  } = useApiTester(endpoint, token)

  return (
    <article className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="rounded-md bg-slate-900 px-2 py-1 text-xs font-semibold text-white">{endpoint.method}</span>
          <h3 className="text-sm font-semibold text-slate-800">{endpoint.name}</h3>
        </div>
        {endpoint.auth ? <span className="text-xs text-amber-600">Cần Bearer token</span> : null}
      </div>

      <p className="mb-3 rounded-md bg-slate-50 px-2 py-1 font-mono text-xs text-slate-700">{endpoint.path}</p>

      {params.length > 0 ? (
        <div className="mb-3 grid gap-2 sm:grid-cols-2">
          {params.map((paramKey) => (
            <label key={paramKey} className="text-xs text-slate-600">
              Path param: {paramKey}
              <input
                value={pathValues[paramKey] ?? ''}
                onChange={(event) => onPathChange(paramKey, event.target.value)}
                className="mt-1 w-full rounded-md border border-slate-300 px-2 py-1 text-sm"
              />
            </label>
          ))}
        </div>
      ) : null}

      <div className="grid gap-3 lg:grid-cols-2">
        <label className="text-xs text-slate-600">
          Query JSON
          <textarea
            rows={7}
            value={queryRaw}
            onChange={(event) => setQueryRaw(event.target.value)}
            className="mt-1 w-full rounded-md border border-slate-300 p-2 font-mono text-xs"
            placeholder='{"page":0,"size":10}'
          />
        </label>

        <label className="text-xs text-slate-600">
          Body JSON
          <textarea
            rows={7}
            value={bodyRaw}
            onChange={(event) => setBodyRaw(event.target.value)}
            className="mt-1 w-full rounded-md border border-slate-300 p-2 font-mono text-xs"
            placeholder='{"field":"value"}'
          />
        </label>
      </div>

      <div className="mt-3">
        <button
          type="button"
          onClick={execute}
          disabled={loading}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? 'Đang gọi...' : 'Call API'}
        </button>
      </div>

      {error ? <p className="mt-3 text-sm text-red-600">{error}</p> : null}

      {response ? (
        <div className="mt-3 space-y-2">
          <p className="text-xs font-semibold text-slate-700">
            Status: <span className={response.ok ? 'text-green-600' : 'text-red-600'}>{response.status}</span>
          </p>
          <JsonBlock value={response.data} />
        </div>
      ) : null}
    </article>
  )
}
