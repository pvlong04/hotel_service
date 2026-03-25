import { useMemo, useState } from 'react'
import { requestApi } from '../../../shared/lib/httpClient'
import { parseJsonOrThrow, safeStringify } from '../utils/json'
import { buildPath, getPathParams } from '../utils/path'

export function useApiTester(endpoint, token) {
  const params = useMemo(() => getPathParams(endpoint.path), [endpoint.path])

  const [pathValues, setPathValues] = useState(() => endpoint.pathSample || {})
  const [queryRaw, setQueryRaw] = useState(() =>
    endpoint.querySample ? safeStringify(endpoint.querySample) : '',
  )
  const [bodyRaw, setBodyRaw] = useState(() =>
    endpoint.bodySample ? safeStringify(endpoint.bodySample) : '',
  )
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)
  const [error, setError] = useState('')

  const onPathChange = (key, value) => {
    setPathValues((prev) => ({
      ...prev,
      [key]: value,
    }))
  }

  const execute = async () => {
    setError('')
    setResponse(null)

    try {
      const query = parseJsonOrThrow(queryRaw, 'Query')
      const body = parseJsonOrThrow(bodyRaw, 'Body')
      const path = buildPath(endpoint.path, pathValues)

      setLoading(true)
      const result = await requestApi({
        method: endpoint.method,
        path,
        token: endpoint.auth ? token : undefined,
        query,
        body,
      })

      setResponse(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra')
    } finally {
      setLoading(false)
    }
  }

  return {
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
  }
}
