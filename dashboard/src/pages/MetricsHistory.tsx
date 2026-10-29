import { useEffect, useMemo, useState } from 'react'
import { Alert, Box, Typography } from '@mui/material'

import MetricsControls from '../components/metrics/MetricsControls'
import MetricsChartCard from '../components/metrics/MetricsChartCard'
import { getDevice, getDevices } from '../services/deviceService'
import type { ApiDeviceSummary, ApiMeasurement,} from '../services/deviceService'
import { getMetricHistory } from '../services/metricService'
import type { ApiMetricHistory } from '../services/metricService'

import {
  CHARTABLE_TYPES,
  RANGES,
  hideUnknownState,
  knownUntilMs,
  resolvePeriod,
  toLocalInput,
} from '../utils/metricHistory'

import type { Period, RangeKey } from '../utils/metricHistory'

import '../styles/MetricsHistory.css'

export default function MetricsHistory() {
  const [devices, setDevices] = useState<ApiDeviceSummary[]>([])
  const [listError, setListError] = useState(false)
  const [deviceId, setDeviceId] = useState('')
  const [deviceMetrics, setDeviceMetrics] = useState<ApiMeasurement[]>([])
  const [metric, setMetric] = useState('')

  const [rangeKey, setRangeKey] = useState<RangeKey>('24h')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')
  const [customError, setCustomError] = useState('')
  const [appliedCustom, setAppliedCustom] = useState<Period | null>(null)
  const [refreshTick, setRefreshTick] = useState(0)

  const [data, setData] = useState<ApiMetricHistory | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)

  // ---- Device list (once, and again on every Load) ----------------------------

  useEffect(() => {
    let cancelled = false

    getDevices()
      .then((list) => {
        if (cancelled) return

        setDevices(list)
        setDeviceId((current) => current || (list[0]?.deviceId ?? ''))
      })
      .catch((loadError) => {
        console.error('Unable to load devices:', loadError)

        if (!cancelled) setListError(true)
      })

    return () => {
      cancelled = true
    }
  }, [])

  // ---- Metrics of the selected device ---------------------------------------------

  useEffect(() => {
    if (!deviceId) return

    let cancelled = false

    setDeviceMetrics([])
    setMetric('')

    getDevice(deviceId)
      .then((details) => {
        if (cancelled) return

        setDeviceMetrics(details.currentValues)

        const first = details.currentValues.find((value) =>
          CHARTABLE_TYPES.includes(value.dataType),
        )

        setMetric(first?.metricName ?? '')
      })
      .catch((loadError) => {
        console.error('Unable to load device metrics:', loadError)

        if (!cancelled) setListError(true)
      })

    return () => {
      cancelled = true
    }
  }, [deviceId])

  const chartableMetrics = useMemo(
    () =>
      deviceMetrics.filter((value) => CHARTABLE_TYPES.includes(value.dataType)),
    [deviceMetrics],
  )

  // ---- History ----------------------------------------------------------------

  useEffect(() => {
    const period = resolvePeriod(rangeKey, appliedCustom)

    if (!deviceId || !metric || !period) {
      setData(null)
      return
    }

    let cancelled = false

    setLoading(true)
    setError(false)

    getMetricHistory(deviceId, metric, period.from, period.to)
      .then((result) => {
        if (!cancelled) setData(result)
      })
      .catch((loadError) => {
        console.error('Unable to load metric history:', loadError)

        if (!cancelled) {
          setError(true)
          setData(null)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [deviceId, metric, rangeKey, appliedCustom, refreshTick])

  // ---- Load button -------------------------------------------------------------------

  function handleLoad() {

    // Brings the status and the last contact of the devices up to date, because
    // they decide how far the state of an On/Off metric is known.
    getDevices()
      .then(setDevices)
      .catch((loadError) => {
        console.error('Unable to refresh devices:', loadError)
      })

    if (rangeKey === 'custom') {
      const from = new Date(customFrom)
      const to = new Date(customTo)

      if (
        !customFrom ||
        !customTo ||
        Number.isNaN(from.getTime()) ||
        Number.isNaN(to.getTime())
      ) {
        setCustomError('Please select a start and end date.')
        return
      }

      if (from >= to) {
        setCustomError('The start date and time must be earlier than the end date and time.')
        return
      }

      setCustomError('')
      setAppliedCustom({ from, to })
    }

    setRefreshTick((tick) => tick + 1)
  }

  // Last data of the selected device, to offer a way to look at it when the period is empty.
  const selectedDevice = devices.find((device) => device.deviceId === deviceId)
  const lastSeen = selectedDevice?.lastSeen ?? null

  // The state of an On/Off metric is only known while the device is connected.
  const knownUntil = knownUntilMs(selectedDevice?.status === 'ONLINE', lastSeen)

  const shownData = useMemo(
    () => hideUnknownState(data, knownUntil),
    [data, knownUntil],
  )

  // Keeps the length of the chosen period but makes it end at the last data.
  function handleShowLastData() {
    if (!lastSeen) return

    const to = new Date(lastSeen)
    const length =
      rangeKey === 'custom'
        ? appliedCustom
          ? appliedCustom.to.getTime() - appliedCustom.from.getTime()
          : 86_400_000
        : (RANGES.find((range) => range.key === rangeKey)?.ms ?? 86_400_000)
    const from = new Date(to.getTime() - length)

    setCustomFrom(toLocalInput(from))
    setCustomTo(toLocalInput(to))
    setCustomError('')
    setAppliedCustom({ from, to })
    setRangeKey('custom')
  }

  const idleMessage =
    rangeKey === 'custom' && !appliedCustom
      ? 'Select a start and end date, then click Load.'
      : 'Select a device and a metric'

  return (
    <Box className="metrics-history">
      <Box className="metrics-history__intro">
        <Typography variant="h1" className="metrics-history__title">
          Metrics &amp; History
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Explore historical measurements and trends for your devices.
        </Typography>
      </Box>

      {listError && (
        <Alert severity="error">
          Unable to load devices or measurements. Please check your connection and try again.
        </Alert>
      )}

      <MetricsControls
        devices={devices}
        deviceId={deviceId}
        onDeviceChange={setDeviceId}
        metrics={chartableMetrics}
        metric={metric}
        onMetricChange={setMetric}
        rangeKey={rangeKey}
        onRangeChange={setRangeKey}
        customFrom={customFrom}
        customTo={customTo}
        onCustomFromChange={setCustomFrom}
        onCustomToChange={setCustomTo}
        customError={customError}
        onLoad={handleLoad}
      />

      <MetricsChartCard
        metric={metric}
        data={shownData}
        loading={loading}
        error={error}
        idleMessage={idleMessage}
        lastSeen={lastSeen}
        knownUntil={knownUntil}
        onShowLastData={handleShowLastData}
      />
    </Box>
  )
}