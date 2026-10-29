import { apiFetch } from './apiClient'

export type HistoryValue = boolean | number | string | null

export interface ApiHistoryPoint {

  timestamp: string

  /* Numeric metrics: average of the time bucket. Boolean/String: the state. */
  value: HistoryValue

  /* Lowest raw measurement inside the bucket (numeric metrics, newer backends only). */
  min?: number | null

  /* Highest raw measurement inside the bucket (numeric metrics, newer backends only). */
  max?: number | null
}

export interface ApiHistoryStatistics {
  min: number | null
  max: number | null
  average: number | null
  latest: HistoryValue
}

export interface ApiMetricHistory {
  deviceId: string
  metricName: string
  dataType: string
  unit: string | null
  from: string
  to: string
  measurementCount: number
  /* "time_bucket_average" for numeric metrics, "state_changes" for Boolean/String metrics. */
  aggregation: string
  bucketSeconds: number | null
  points: ApiHistoryPoint[]
  statistics: ApiHistoryStatistics
}

export function getMetricHistory(
  deviceId: string,
  metricName: string,
  from: Date,
  to: Date,
): Promise<ApiMetricHistory> {
  const params = new URLSearchParams({
    deviceId,
    metricName,
    from: from.toISOString(),
    to: to.toISOString(),
  })

  return apiFetch<ApiMetricHistory>(`/metrics/history?${params.toString()}`)
}