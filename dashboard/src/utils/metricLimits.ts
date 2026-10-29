/**
 * Limits that trigger an alert on a "Current Values" tile.
 *
 * - low:  the tile is flagged when value <= low
 * - high: the tile is flagged when value >= high
 *
 * Only metrics listed here are checked. A metric whose limits are unknown is
 * simply left out and is never flagged.
 */
export type LimitState = 'low' | 'high' | null

type MetricLimit = { low?: number; high?: number }

const METRIC_LIMITS: Record<string, MetricLimit> = {
  tankLevel: { low: 0 },
}

export function getLimitState(metricName: string, value: number): LimitState {
  const limit = METRIC_LIMITS[metricName]

  if (!limit) return null

  if (limit.low !== undefined && value <= limit.low) return 'low'
  if (limit.high !== undefined && value >= limit.high) return 'high'

  return null
}