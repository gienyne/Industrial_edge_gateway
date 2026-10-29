const API_BASE_URL = '/api'

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

/* Error thrown for every failed request; status is the HTTP status code (0 = server unreachable). */
export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

let unauthorizedHandler: (() => void) | null = null

/* Called when the backend answers 401 (session expired): the app then shows the login page. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler
}

function readCsrfToken(): string | null {
  const entry = document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(`${CSRF_COOKIE}=`))

  return entry ? decodeURIComponent(entry.substring(CSRF_COOKIE.length + 1)) : null
}

/* The backend only sets the CSRF cookie when asked; this asks once if it is missing. */
async function ensureCsrfToken(): Promise<string | null> {
  const existing = readCsrfToken()

  if (existing) return existing

  try {
    await fetch(`${API_BASE_URL}/auth/csrf`, { credentials: 'same-origin' })
  } catch {
    // Server unreachable: the actual request below fails and reports it to the user.
    return null
  }

  return readCsrfToken()
}

/**
 * Builds the text shown to the user for a failed request.
 * Backend messages are preserved for client errors (4xx) when available.
 * Server errors (5xx) use frontend messages.
 */
async function readErrorMessage(response: Response, path: string): Promise<string> {
  if (response.status < 500) {
    try {
      const body: unknown = await response.json()

      if (
        body &&
        typeof body === 'object' &&
        'message' in body &&
        typeof body.message === 'string' &&
        body.message.trim()
      ) {
        return body.message
      }
    } catch {
      // If the response body cannot be parsed as JSON, fallback messages below will be use
    }
  }

  switch (response.status) {
    case 400:
      return 'Please check the information you entered and try again.'
    case 401:
      // On the login form a 401 means wrong credentials; anywhere else the session ended.
      return path === '/auth/login'
        ? 'The username or password you entered is incorrect.'
        : 'Your session has expired. Please sign in again.'
    case 403:
      return "You don't have permission to perform this action."
    case 404:
      return 'The requested information could not be found.'
    case 429:
      return 'Too many attempts. Please wait a moment and try again.'
    case 502:
    case 503:
    case 504:
      return 'The service is currently unavailable. Please try again in a moment.'
    default:
      return 'Something went wrong. Please try again later.'
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const headers = new Headers(options.headers)

  headers.set('Accept', 'application/json')

  // Requests that change something must carry the CSRF token.
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = await ensureCsrfToken()

    if (token) {
      headers.set(CSRF_HEADER, token)
    }
  }

  let response: Response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      credentials: 'same-origin',
      headers,
    })
  } catch {
    // fetch only throws when no answer arrives at all (backend down, network lost).
    throw new ApiError(0, 'Unable to reach the server. Please check your connection and try again.',)
  }

  if (!response.ok) {
    // A wrong password on the login form is also a 401, but it is not an expired session.
    if (response.status === 401 && path !== '/auth/login') {
      unauthorizedHandler?.()
    }

    throw new ApiError(response.status, await readErrorMessage(response, path))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}