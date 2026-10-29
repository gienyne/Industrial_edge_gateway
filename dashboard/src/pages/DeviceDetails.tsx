import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import {Alert, Box, Button, CircularProgress, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,Typography,} from '@mui/material'
import ArrowBackOutlined from '@mui/icons-material/ArrowBackOutlined'
import MemoryOutlined from '@mui/icons-material/MemoryOutlined'
import RefreshOutlined from '@mui/icons-material/RefreshOutlined'
import SensorsOutlined from '@mui/icons-material/SensorsOutlined'
import OpacityOutlined from '@mui/icons-material/OpacityOutlined'
import WaterDropOutlined from '@mui/icons-material/WaterDropOutlined'
import PowerSettingsNewOutlined from '@mui/icons-material/PowerSettingsNewOutlined'
import TuneOutlined from '@mui/icons-material/TuneOutlined'
import CloudOutlined from '@mui/icons-material/CloudOutlined'
import ThermostatOutlined from '@mui/icons-material/ThermostatOutlined'
import TimerOutlined from '@mui/icons-material/TimerOutlined'
import TouchAppOutlined from '@mui/icons-material/TouchAppOutlined'
import SpeedOutlined from '@mui/icons-material/SpeedOutlined'

import StatusChip from '../components/StatusChip'
import MetricHistoryChart from '../components/devices/MetricHistoryChart'

import {getDevice, getRecentMeasurements,} from '../services/deviceService'

import type { ApiDeviceDetails, ApiMeasurement, } from '../services/deviceService'
import { getLimitState } from '../utils/metricLimits'

import '../styles/DeviceDetails.css'

type DeviceDetailsProps = {
  deviceId: string
  onBack: () => void
}

type Tone = | 'level' | 'water' | 'humidity' | 'power' | 'valve' | 'rain' | 'temp' | 'time'| 'force' | 'neutral'

function formatDate(value: string | null, withSeconds = false): string {
  if (!value) return 'Never'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: withSeconds ? 'medium' : 'short',
  }).format(date)
}

/* Booleans as Yes/No, numbers rounded to 2 decimals, everything else as text. */
function formatValue(measurement: ApiMeasurement): string {
  const { value } = measurement

  if (value === null) return '—'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'

  if (typeof value === 'number') {
    return new Intl.NumberFormat(undefined, {
      maximumFractionDigits: 2,
    }).format(value)
  }

  return String(value)
}

/* Chooses an icon and a color tone from the metric name (each device brings its own metrics). */
function metricVisual(metricName: string): { icon: ReactNode; tone: Tone } {
  const name = metricName.toLowerCase()

  if (name.includes('level')) return { icon: <OpacityOutlined />, tone: 'level' }
  if (name.includes('consumption')) {
    return { icon: <WaterDropOutlined />, tone: 'water' }
  }
  if (name.includes('humid')) {
    return { icon: <WaterDropOutlined />, tone: 'humidity' }
  }
  if (name.includes('pump')) {
    return { icon: <PowerSettingsNewOutlined />, tone: 'power' }
  }
  if (name.includes('valve')) return { icon: <TuneOutlined />, tone: 'valve' }
  if (name.includes('rain')) return { icon: <CloudOutlined />, tone: 'rain' }
  if (name.includes('temp')) {
    return { icon: <ThermostatOutlined />, tone: 'temp' }
  }
  if (name.includes('time') || name.includes('uptime')) {
    return { icon: <TimerOutlined />, tone: 'time' }
  }
  if (name.includes('force') || name.includes('pressure')) {
    return { icon: <SpeedOutlined />, tone: 'force' }
  }
  if (name.includes('button')) {
    return { icon: <TouchAppOutlined />, tone: 'neutral' }
  }

  return { icon: <SensorsOutlined />, tone: 'neutral' }
}

function BooleanPill({ value }: { value: boolean }) {
  return (
    <span className="device-details__pill" data-on={value}>
      {value ? 'Yes' : 'No'}
    </span>
  )
}

export default function DeviceDetails({
  deviceId,
  onBack,
}: DeviceDetailsProps) {
  const [device, setDevice] = useState<ApiDeviceDetails | null>(null)
  const [measurements, setMeasurements] = useState<ApiMeasurement[]>([])
  const [loading, setLoading] = useState(true)
  const [apiError, setApiError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  // Loads the device once with a spinner, then refreshes every 2 seconds
  // (same rhythm as the Overview and Devices pages).
  useEffect(() => {
    let cancelled = false
    let timeoutId: number | undefined

    async function loadDetails(initial: boolean) {
      if (initial) {
        setLoading(true)
        setApiError(false)
        setDevice(null)
        setMeasurements([])
      }

      try {
        const [deviceData, measurementData] = await Promise.all([
          getDevice(deviceId),
          getRecentMeasurements(deviceId),
        ])

        if (!cancelled) {
          setDevice(deviceData)
          setMeasurements(measurementData)
          setApiError(false)
        }
      } catch (error) {
        console.error('Unable to load device details:', error)

        if (!cancelled) {
          setApiError(true)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)

          timeoutId = window.setTimeout(() => {
            void loadDetails(false)
          }, 2000)
        }
      }
    }

    void loadDetails(true)

    return () => {
      cancelled = true

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId)
      }
    }
  }, [deviceId, reloadKey])

  return (
    <Box className="device-details">
      <Button
        startIcon={<ArrowBackOutlined />}
        onClick={onBack}
        className="device-details__back"
      >
        Back to Devices
      </Button>

      <Typography variant="h1" className="device-details__title">
        Device Details - {deviceId}
      </Typography>

      {apiError && (
        <Alert
          severity="error"
          action={
            <Button
              color="inherit"
              size="small"
              startIcon={<RefreshOutlined />}
              onClick={() => setReloadKey((key) => key + 1)}
            >
              Retry
            </Button>
          }
          className="device-details__error"
        >
          Unable to load device details. Please try again.
        </Alert>
      )}

      {loading ? (
        <Box className="device-details__loading">
          <CircularProgress size={24} />
          <Typography color="text.secondary">
            Loading device details...
          </Typography>
        </Box>
      ) : device ? (
        <>
          {/* Identity + general facts */}
          <Box className="device-details__top">
            <Paper
              variant="outlined"
              className="device-details__card device-details__identity"
            >
              <Box className="device-details__device-icon">
                <MemoryOutlined />
              </Box>

              <Box className="device-details__identity-text">
                <Typography className="device-details__device-name">
                  {device.deviceId}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Industrial device
                </Typography>
              </Box>

              <StatusChip status={device.status} />
            </Paper>

            <Paper
              variant="outlined"
              className="device-details__card device-details__facts"
            >
              <Box className="device-details__fact">
                <span className="device-details__fact-label">Group</span>
                <span className="device-details__fact-value">
                  {device.groupId || 'Not assigned'}
                </span>
              </Box>

              <Box className="device-details__fact">
                <span className="device-details__fact-label">Edge Node</span>
                <span className="device-details__fact-value">
                  {device.edgeNodeId || 'Not assigned'}
                </span>
              </Box>

              <Box className="device-details__fact">
                <span className="device-details__fact-label">Last Seen</span>
                <span className="device-details__fact-value">
                  {formatDate(device.lastSeen)}
                </span>
              </Box>
            </Paper>
          </Box>

          {/* Current values: always the metrics of THIS device */}
          <Paper
            variant="outlined"
            className="device-details__card device-details__values-card"
          >
            <Box className="device-details__section-header">
              <Typography variant="h6" className="device-details__section-title">
                Current Values
              </Typography>

              <Typography className="device-details__count">
                {device.currentValues.length}
              </Typography>
            </Box>

            {device.currentValues.length === 0 ? (
              <Typography
                variant="body2"
                color="text.secondary"
                className="device-details__empty"
              >
                No current values reported yet
              </Typography>
            ) : (
              <Box className="device-details__values">
                {device.currentValues.map((metric) => {
                  const visual = metricVisual(metric.metricName)
                  const alert =
                    typeof metric.value === 'number'
                      ? getLimitState(metric.metricName, metric.value)
                      : null

                  return (
                    <Box
                      key={metric.metricName}
                      className="device-details__value-tile"
                      data-tone={visual.tone}
                      data-alert={alert ?? undefined}
                      title={
                        alert === 'low'
                          ? 'Below the lower limit'
                          : alert === 'high'
                            ? 'Above the upper limit'
                            : undefined
                      }
                    >
                      <Box className="device-details__value-header">
                        <Box className="device-details__value-icon">
                          {visual.icon}
                        </Box>

                        <span
                          className="device-details__value-name"
                          title={metric.metricName}
                        >
                          {metric.metricName}
                        </span>

                        {alert && (
                          <span className="device-details__alert-badge">
                            {alert === 'low' ? 'Low' : 'High'}
                          </span>
                        )}
                      </Box>

                      <Box className="device-details__value-body">
                        {typeof metric.value === 'boolean' ? (
                          <BooleanPill value={metric.value} />
                        ) : (
                          <>
                            <span className="device-details__value-number">
                              {formatValue(metric)}
                            </span>

                            {metric.unit && (
                              <span className="device-details__value-unit">
                                {metric.unit}
                              </span>
                            )}
                          </>
                        )}
                      </Box>
                    </Box>
                  )
                })}
              </Box>
            )}
          </Paper>

          {/* History chart + recent measurements */}
          <Box className="device-details__lower">
            <MetricHistoryChart
              key={device.deviceId}
              deviceId={device.deviceId}
              metrics={device.currentValues}
              live={device.status === 'ONLINE'}
              endTime={device.lastSeen}
            />

            <Paper
              variant="outlined"
              className="device-details__card device-details__measurements"
            >
              <Box className="device-details__section-header">
                <Box>
                  <Typography
                    variant="h6"
                    className="device-details__section-title"
                  >
                    Recent Measurements
                  </Typography>

                  <Typography variant="body2" color="text.secondary">
                    Latest values reported by this device
                  </Typography>
                </Box>

                <Typography className="device-details__count">
                  {measurements.length}
                </Typography>
              </Box>

              <TableContainer className="device-details__table-container">
                <Table
                  stickyHeader
                  size="small"
                  aria-label="Recent device measurements"
                >
                  <TableHead>
                    <TableRow>
                      <TableCell>Time</TableCell>
                      <TableCell>Metric</TableCell>
                      <TableCell>Value</TableCell>
                      <TableCell>Unit</TableCell>
                    </TableRow>
                  </TableHead>

                  <TableBody>
                    {measurements.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={4} align="center">
                          <Typography
                            variant="body2"
                            color="text.secondary"
                          >
                            No recent measurements found
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ) : (
                      measurements.map((measurement, index) => (
                        <TableRow
                          key={`${measurement.metricName}-${measurement.timestamp}-${index}`}
                          hover
                        >
                          <TableCell className="device-details__timestamp">
                            {formatDate(measurement.timestamp, true)}
                          </TableCell>
                          <TableCell className="device-details__metric-name">
                            {measurement.metricName}
                          </TableCell>
                          <TableCell className="device-details__metric-value">
                            {typeof measurement.value === 'boolean' ? (
                              <BooleanPill value={measurement.value} />
                            ) : (
                              formatValue(measurement)
                            )}
                          </TableCell>
                          <TableCell className="device-details__unit">
                            {measurement.unit ?? '—'}
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Box>
        </>
      ) : null}
    </Box>
  )
}