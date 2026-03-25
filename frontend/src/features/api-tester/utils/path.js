const PARAM_REGEX = /:([A-Za-z0-9_]+)/g

export function getPathParams(pathTemplate) {
  return [...pathTemplate.matchAll(PARAM_REGEX)].map((entry) => entry[1])
}

export function buildPath(pathTemplate, pathParams) {
  return pathTemplate.replace(PARAM_REGEX, (_, key) => {
    const value = pathParams[key]
    return encodeURIComponent(value || `:${key}`)
  })
}
