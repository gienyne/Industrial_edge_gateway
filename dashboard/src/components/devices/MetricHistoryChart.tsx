import { useEffect, useMemo, useState } from 'react'
import {Box, Button, MenuItem, Paper, Select, ToggleButton, ToggleButtonGroup, Typography,} from '@mui/material'
import HistoryPlot from '../metrics/HistoryPlot'
import { getMetricHistory } from '../../services/metricService'
import type { ApiMetricHistory } from '../../services/metricService'
import type { ApiMeasurement } from '../../services/deviceService'
import { formatStat } from '../../utils/historyFormat'
import {CHARTABLE_TYPES, hideUnknownState, knownUntilMs,} from '../../utils/metricHistory'

import '../../styles/MetricHistoryChart.css'

type MetricHistoryChartProps = {

  deviceId: string

  /* The device's own metrics (its current values). */
  metrics: ApiMeasurement[]

  /*True while the device is online: the chart then follows "now" and refreshes. */
  live: boolean

  /* Last time the device was seen. Offered as an option when the chosen period has no data. */
  endTime: string | null
}

function formatMoment(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'medium',
  }).format(date)
}

type RangeKey = '1h' | '6h' | '24h' | '7d'

const RANGES: { key: RangeKey; label: string; ms: number }[] = [
  { key: '1h', label: '1h', ms: 3_600_000 },
  { key: '6h', label: '6h', ms: 21_600_000 },
  { key: '24h', label: '24h', ms: 86_400_000 },
  { key: '7d', label: '7d', ms: 604_800_000 },
]

export default function MetricHistoryChart({
  deviceId,
  metrics,
  live,
  endTime,
}: MetricHistoryChartProps) {
  const chartable = useMemo(
    () =>
      metrics
        .filter((metric) => CHARTABLE_TYPES.includes(metric.dataType))
        .map((metric) => metric.metricName),
    [metrics],
  )

  const [selectedMetric, setSelectedMetric] = useState<string>('')
  const [rangeKey, setRangeKey] = useState<RangeKey>('24h')
  const [data, setData] = useState<ApiMetricHistory | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)

  // Off by default: "1h" always means the last hour before now.
  const [showLastData, setShowLastData] = useState(false)

  // Picks a default metric (and fixes the choice if it disappears).
  const metric = chartable.includes(selectedMetric) ? selectedMetric : (chartable[0] ?? '')

  const rangeMs = RANGES.find((range) => range.key === rangeKey)?.ms ?? 86_400_000

  // Only when the user asks for it, and only for a device that is not sending data any more.
  const anchorMs = !live && showLastData && endTime ? new Date(endTime).getTime() : null

  useEffect(() => {
    if (!metric) return

    let cancelled = false
    let timeoutId: number | undefined

    async function load(initial: boolean) {
      if (initial) {
        setLoading(true)
        setData(null)
      }

      try {

        const to = anchorMs !== null && Number.isFinite(anchorMs) ? new Date(anchorMs) : new Date()

        const from = new Date(to.getTime() - rangeMs)

        const result = await getMetricHistory(deviceId, metric, from, to)

        if (!cancelled) {
          setData(result)
          setError(false)
        }
      } catch (loadError) {
        console.error('Unable to load metric history:', loadError)

        if (!cancelled) {
          setError(true)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)

          if (live) {
            timeoutId = window.setTimeout(() => {
              void load(false)
            }, 5000)
          }
        }
      }
    }

    void load(true)

    return () => {
      cancelled = true

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId)
      }
    }
  }, [deviceId, metric, rangeMs, anchorMs, live])

  // The state of an On/Off metric is only known while the device is connected:
  // after its last contact the chart shows no state instead of a flat line.
  const knownUntil = knownUntilMs(live, endTime)

  const shown = useMemo(
    () => hideUnknownState(data, knownUntil),
    [data, knownUntil],
  )

  const unit = shown?.unit ?? null
  const statistics = shown?.statistics
  const hasPoints = shown !== null && shown.points.length > 0
  const isBoolean = shown?.dataType === 'Boolean'

  return (
    <Paper variant="outlined" className="metric-chart">
      <Box className="metric-chart__header">
        <Box className="metric-chart__title-group">
          <Typography variant="h6" className="metric-chart__title">
            Metric History
          </Typography>

          {chartable.length > 0 && (
            <Select
              size="small"
              value={metric}
              onChange={(event) => setSelectedMetric(event.target.value)}
              className="metric-chart__select"
              inputProps={{ 'aria-label': 'Metric to display' }}
            >
              {chartable.map((name) => (
                <MenuItem key={name} value={name}>
                  {name}
                </MenuItem>
              ))}
            </Select>
          )}
        </Box>

        <ToggleButtonGroup
          exclusive
          size="small"
          value={rangeKey}
          onChange={(_, value: RangeKey | null) => {
            if (value) setRangeKey(value)
          }}
          className="metric-chart__ranges"
          aria-label="Time range"
        >
          {RANGES.map((range) => (
            <ToggleButton key={range.key} value={range.key}>
              {range.label}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
      </Box>

      {!live &&
        endTime &&
        !showLastData &&
        shown !== null &&
        shown.points.length === 0 && (
          <Box className="metric-chart__notice">
            <span>
              No data in this period. Last measurement: {formatMoment(endTime)}
            </span>

            <Button size="small" onClick={() => setShowLastData(true)}>
              Show last data
            </Button>
          </Box>
        )}

      {!live && endTime && showLastData && (
        <Box className="metric-chart__notice" data-mode="last-data">
          <span>
            Showing the period that ends at the last measurement (
            {formatMoment(endTime)}).
          </span>

          <Button size="small" onClick={() => setShowLastData(false)}>
            Back to now
          </Button>
        </Box>
      )}

      <HistoryPlot
        data={chartable.length === 0 ? null : shown}
        loading={loading}
        error={error}
        idleMessage={
          chartable.length === 0 ? 'This device has no metric that can be charted' : 'No data to display'
        }
        knownUntil={knownUntil}
      />

      {hasPoints && statistics && (
        <Box className="metric-chart__stats">
          {isBoolean || typeof statistics.min !== 'number' ? (
            <>
              <Box
                className="metric-chart__stat"
                data-tone="latest"
                title="Most recent value in the period"
              >
                <span className="metric-chart__stat-label">Latest</span>
                <span className="metric-chart__stat-value">
                  {formatStat(statistics.latest, unit)}
                </span>
              </Box>
              <Box
                className="metric-chart__stat"
                data-tone="changes"
                title="Number of times the state changed"
              >
                <span className="metric-chart__stat-label">State changes</span>
                <span className="metric-chart__stat-value">
                  {Math.max(0, (shown?.points.length ?? 0) - 1)}
                </span>
              </Box>
              <Box
                className="metric-chart__stat"
                data-tone="samples"
                title="Number of raw measurements stored in the period"
              >
                <span className="metric-chart__stat-label">Samples</span>
                <span className="metric-chart__stat-value">
                  {shown?.measurementCount}
                </span>
              </Box>
            </>
          ) : (
            <>
              <Box
                className="metric-chart__stat"
                data-tone="min"
                title="Lowest raw measurement in the period"
              >
                <span className="metric-chart__stat-label">Min</span>
                <span className="metric-chart__stat-value">
                  {formatStat(statistics.min, unit)}
                </span>
              </Box>
              <Box
                className="metric-chart__stat"
                data-tone="average"
                title="Average of all raw measurements in the period"
              >
                <span className="metric-chart__stat-label">Average</span>
                <span className="metric-chart__stat-value">
                  {formatStat(statistics.average, unit)}
                </span>
              </Box>
              <Box
                className="metric-chart__stat"
                data-tone="max"
                title="Highest raw measurement in the period"
              >
                <span className="metric-chart__stat-label">Max</span>
                <span className="metric-chart__stat-value">
                  {formatStat(statistics.max, unit)}
                </span>
              </Box>
              <Box
                className="metric-chart__stat"
                data-tone="latest"
                title="Most recent value in the period"
              >
                <span className="metric-chart__stat-label">Latest</span>
                <span className="metric-chart__stat-value">
                  {formatStat(statistics.latest, unit)}
                </span>
              </Box>
              <Box
                className="metric-chart__stat"
                data-tone="samples"
                title="Number of raw measurements stored in the period"
              >
                <span className="metric-chart__stat-label">Samples</span>
                <span className="metric-chart__stat-value">
                  {shown?.measurementCount}
                </span>
              </Box>
            </>
          )}
        </Box>
      )}
    </Paper>
  )
}