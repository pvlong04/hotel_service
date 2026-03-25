export function safeStringify(value) {
  return JSON.stringify(value, null, 2)
}

export function parseJsonOrThrow(raw, label) {
  if (!raw || !raw.trim()) {
    return undefined
  }

  try {
    return JSON.parse(raw)
  } catch {
    throw new Error(`${label} phải là JSON hợp lệ`)
  }
}
