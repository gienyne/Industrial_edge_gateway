import { apiFetch } from './apiClient'
import type { ApiOverviewResponse } from '../types/overview'

export function getOverview(): Promise<ApiOverviewResponse> {
  return apiFetch<ApiOverviewResponse>('/overview')
}