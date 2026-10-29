/* Shared by the Metrics & History page and its components. */

import type { ApiMetricHistory } from '../services/metricService'

export type RangeKey = '1h' | '6h' | '24h' | '7d' | '30d' | 'custom'

export type Period = { from: Date; to: Date }

/* Metric types that can be drawn as a chart (text metrics cannot). */
export const CHARTABLE_TYPES = ['Integer', 'Double', 'Boolean']

export const RANGES: {
  key: Exclude<RangeKey, 'custom'>
  label: string
  ms: number
}[] = [
  { key: '1h', label: '1h', ms: 3_600_000 },
  { key: '6h', label: '6h', ms: 21_600_000 },
  { key: '24h', label: '24h', ms: 86_400_000 },
  { key: '7d', label: '7d', ms: 604_800_000 },
  { key: '30d', label: '30d', ms: 2_592_000_000 },
]

/* Formats a date for a datetime-local input ("2026-10-23T10:57", local time). */
export function toLocalInput(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/* Turns the selected range into real dates ("now" for the presets). */
export function resolvePeriod(
  key: RangeKey,
  custom: Period | null,
): Period | null {
  if (key === 'custom') return custom

  const ms = RANGES.find((range) => range.key === key)?.ms ?? 86_400_000
  const to = new Date()

  return { from: new Date(to.getTime() - ms), to }
}

/**
 * Last moment for which the state of a device is known: without limit while the
 * device is online, otherwise the moment it was last seen (nothing is known if
 * it was never seen).
 */
export function knownUntilMs(online: boolean, lastSeen: string | null): number {
  if (online) return Number.POSITIVE_INFINITY

  const seen = lastSeen ? new Date(lastSeen).getTime() : Number.NaN

  return Number.isFinite(seen) ? seen : Number.NEGATIVE_INFINITY
}

/**
 * A state history (On/Off) starts with the state the metric had when the period
 * began, even if no measurement was stored in the period. That state is only a
 * fact while the device was connected. If the device was last seen before the
 * period even started, nothing is known about the period: it counts as empty
 * and the charts show "No data in this period".
 */
export function hideUnknownState(
  data: ApiMetricHistory | null,
  knownUntil: number,
): ApiMetricHistory | null {
  if (
    !data ||
    data.aggregation !== 'state_changes' ||
    data.measurementCount > 0
  ) {
    return data
  }

  return knownUntil <= new Date(data.from).getTime()
    ? { ...data, points: [] }
    : data
}