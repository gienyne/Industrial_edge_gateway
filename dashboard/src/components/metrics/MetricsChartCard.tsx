import type { ReactNode } from 'react'
import { Box, Button, Paper, Typography } from '@mui/material'
import ArrowDownwardOutlined from '@mui/icons-material/ArrowDownwardOutlined'
import ArrowUpwardOutlined from '@mui/icons-material/ArrowUpwardOutlined'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import FunctionsOutlined from '@mui/icons-material/FunctionsOutlined'
import AccessTimeOutlined from '@mui/icons-material/AccessTimeOutlined'
import SwapVertOutlined from '@mui/icons-material/SwapVertOutlined'
import BarChartOutlined from '@mui/icons-material/BarChartOutlined'

import HistoryPlot from './HistoryPlot'
import type { ApiMetricHistory } from '../../services/metricService'
import { formatBucket, formatStat } from '../../utils/historyFormat'

import '../../styles/MetricsChartCard.css'

type MetricsChartCardProps = {
  metric: string
  data: ApiMetricHistory | null
  loading: boolean
  error: boolean

  /* Shown while there is nothing to display yet. */
  idleMessage: string

  /* Last time the selected device sent data (null if unknown). */
  lastSeen: string | null

  /* Last moment (ms) for which the state of the device is known. */
  knownUntil: number

  /* Moves the period so that it ends at the last data. */
  onShowLastData: () => void
}

type Stat = {
  tone: 'min' | 'max' | 'average' | 'latest' | 'changes' | 'samples'
  label: string
  value: string
  hint: string
  icon: ReactNode
}

function formatDateTime(value: string): string {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'short',
    timeStyle: 'medium',
  }).format(date)
}

export default function MetricsChartCard({
  metric,
  data,
  loading,
  error,
  idleMessage,
  lastSeen,
  knownUntil,
  onShowLastData,
}: MetricsChartCardProps) {
  const unit = data?.unit ?? null
  const statistics = data?.statistics
  const hasPoints = data !== null && data.points.length > 0
  const isNumeric = typeof statistics?.min === 'number'

  const subtitle = data ? (
    <>

       {formatDateTime(data.from)}
       <ArrowForwardIcon
         fontSize="inherit"
         sx={{ verticalAlign: 'middle', mx: 0.5 }}
       />

       {formatDateTime(data.to)}
       {' · '}
       {data.aggregation === 'time_bucket_average' 
       ? `Average per ${formatBucket(data.bucketSeconds)}` 
       : 'State changes'}

    </>
  ) : (
    idleMessage
  )

  let stats: Stat[] = []

  if (hasPoints && statistics && data) {
    stats = isNumeric
      ? [
          {
            tone: 'min',
            label: 'Min',
            value: formatStat(statistics.min, unit),
            hint: 'Lowest raw measurement in the period',
            icon: <ArrowDownwardOutlined />,
          },
          {
            tone: 'max',
            label: 'Max',
            value: formatStat(statistics.max, unit),
            hint: 'Highest raw measurement in the period',
            icon: <ArrowUpwardOutlined />,
          },
          {
            tone: 'average',
            label: 'Average',
            value: formatStat(statistics.average, unit),
            hint: 'Average of all raw measurements in the period',
            icon: <FunctionsOutlined />,
          },
          {
            tone: 'latest',
            label: 'Latest',
            value: formatStat(statistics.latest, unit),
            hint: 'Most recent value in the period',
            icon: <AccessTimeOutlined />,
          },
        ]
      : [
          {
            tone: 'latest',
            label: 'Latest',
            value: formatStat(statistics.latest, unit),
            hint: 'Most recent state in the period',
            icon: <AccessTimeOutlined />,
          },
          {
            tone: 'changes',
            label: 'State changes',
            value: String(Math.max(0, data.points.length - 1)),
            hint: 'Number of times the state changed',
            icon: <SwapVertOutlined />,
          },
          {
            tone: 'samples',
            label: 'Samples',
            value: String(data.measurementCount),
            hint: 'Number of raw measurements stored in the period',
            icon: <BarChartOutlined />,
          },
        ]
  }

  return (
    <Paper variant="outlined" className="metrics-chart-card">
      <Box className="metrics-chart-card__header">
        <Box>
          <Typography variant="h6" className="metrics-chart-card__title">
            {metric || 'No metric selected'}
            {unit ? ` (${unit})` : ''}
          </Typography>

          <Typography variant="body2" color="text.secondary">
            {subtitle}
          </Typography>
        </Box>
      </Box>

      {data !== null && data.points.length === 0 && lastSeen && (
        <Box className="metrics-chart-card__notice">
          <span>
            No data in this period. Last data from this device:{' '}
            {formatDateTime(lastSeen)}
          </span>

          <Button size="small" onClick={onShowLastData}>
            Show the period ending there
          </Button>
        </Box>
      )}

      <HistoryPlot
        data={data}
        loading={loading}
        error={error}
        idleMessage={idleMessage}
        knownUntil={knownUntil}
      />

      {stats.length > 0 && (
        <Box className="metrics-chart-card__stats">
          {stats.map((stat) => (
            <Box
              key={stat.label}
              className="metrics-chart-card__stat"
              data-tone={stat.tone}
              title={stat.hint}
            >
              <Box className="metrics-chart-card__stat-icon">{stat.icon}</Box>

              <Box className="metrics-chart-card__stat-text">
                <span className="metrics-chart-card__stat-label">
                  {stat.label}
                </span>
                <span className="metrics-chart-card__stat-value">
                  {stat.value}
                </span>
              </Box>
            </Box>
          ))}
        </Box>
      )}
    </Paper>
  )
}