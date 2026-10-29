import { apiFetch } from './apiClient'
import type { ApiEventPage, EventSeverity, EventType } from '../types/event'

export type EventQuery = {
  type?: EventType
  severity?: EventSeverity
  deviceId?: string
  from?: Date
  to?: Date
}

/* Newest events first. `page` starts at 0. */
export function getEvents(
  query: EventQuery,
  page: number,
  size: number,
): Promise<ApiEventPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  })

  if (query.type) params.set('type', query.type)
  if (query.severity) params.set('severity', query.severity)
  if (query.deviceId) params.set('deviceId', query.deviceId)
  if (query.from) params.set('from', query.from.toISOString())
  if (query.to) params.set('to', query.to.toISOString())

  return apiFetch<ApiEventPage>(`/events?${params.toString()}`)
}