import { apiFetch } from './apiClient'

export interface ApiLoginResponse {
  userId: string
}

/* Rejects with a 401 ApiError when nobody is signed in. */
export function getCurrentUser(): Promise<ApiLoginResponse> {
  return apiFetch<ApiLoginResponse>('/auth/me')
}

export function login(
  userId: string,
  password: string,
): Promise<ApiLoginResponse> {
  return apiFetch<ApiLoginResponse>('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, password }),
  })
}

export function logout(): Promise<void> {
  return apiFetch<void>('/auth/logout', { method: 'POST' })
}

/* Rejects with a 400 ApiError when the current password is wrong or the new one is refused. */
export function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  return apiFetch<void>('/auth/password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ currentPassword, newPassword }),
  })
}