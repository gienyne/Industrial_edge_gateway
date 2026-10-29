const numberFormat = new Intl.NumberFormat(undefined, {
  maximumFractionDigits: 2,
})

export function formatPlain(value: number): string {
  return numberFormat.format(value)
}

/* Formats a statistic or a value, with its unit when it has one. */
export function formatStat(
  value: number | boolean | string | null | undefined,
  unit: string | null,
): string {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'string') return value

  return unit ? `${formatPlain(value)} ${unit}` : formatPlain(value)
}

/* "300" -> "5 min", "3600" -> "1 h" ... */
export function formatBucket(seconds: number | null): string {
  if (seconds === null) return ''
  if (seconds < 60) return `${seconds} s`
  if (seconds < 3600) return `${seconds / 60} min`
  if (seconds < 86_400) return `${seconds / 3600} h`

  return `${seconds / 86_400} d`
}