import { useEffect, useState } from 'react'
import { Alert, Box, CircularProgress, Typography } from '@mui/material'

import HealthSummary from '../components/overview/HealthSummary'
import DeviceStatusSummary from '../components/overview/DeviceStatusSummary'
import DeviceTable from '../components/overview/DeviceTable'
import LiveActivity from '../components/overview/LiveActivity'

import type { OverviewDevice } from '../components/overview/DeviceTable'
import type { ApiEvent } from '../components/overview/LiveActivity'
import type { ApiOverviewResponse } from '../types/overview'

import { getOverview } from '../services/overviewService'

import '../styles/Overview.css'

export default function Overview() {
  const [overview, setOverview] = useState<ApiOverviewResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [apiError, setApiError] = useState(false)

  useEffect(() => {
    let cancelled = false
    let timeoutId: number | undefined

    async function loadOverview() {
      try {
        const data = await getOverview()

        if (!cancelled) {
          setOverview(data)
          setApiError(false)
        }
      } catch (error) {
        console.error('Unable to load overview from API:', error)

        if (!cancelled) {
          setApiError(true)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)

          timeoutId = window.setTimeout(() => {
            void loadOverview()
          }, 2000)
        }
      }
    }

    void loadOverview()

    return () => {
      cancelled = true

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId)
      }
    }
  }, [])

  const devices: OverviewDevice[] = overview?.devices ?? []
  const activities: ApiEvent[] = overview?.recentActivities ?? []

  const count = (status: OverviewDevice['status']) =>
    devices.filter((device) => device.status === status).length

  const systemHealth = overview
    ? overview.systemHealth === 'HEALTHY'
      ? 'Healthy'
      : 'Degraded'
    : 'Unknown'

  const mqttConnection = overview
    ? overview.mqttConnection === 'CONNECTED'
      ? 'Connected'
      : 'Disconnected'
    : 'Unknown'

  const databaseConnection = overview
    ? overview.databaseConnection === 'CONNECTED'
      ? 'Connected'
      : 'Disconnected'
    : 'Unknown'

  return (
    <Box className="overview">
      <Box className="overview__intro">
        <Box className="overview__heading">
          <Typography variant="h1" className="overview__title">
            System Overview
          </Typography>

          {/* Shown for a short time after the backend reconnects to the MQTT broker. */}
          {overview?.synchronizing && (
            <span
              className="overview__sync"
              role="status"
              title="The backend has just reconnected to the MQTT broker. Devices stay Unknown until their birth message confirms their status."
            >
              <CircularProgress size={14} thickness={5} color="inherit" />
              Synchronizing…
            </span>
          )}
        </Box>
      </Box>

      {apiError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Unable to load system overview. Please try later.
        </Alert>
      )}

      {loading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <CircularProgress size={18} />
          <Typography color="text.secondary" variant="body2">
            Loading overview...
          </Typography>
        </Box>
      )}

      <HealthSummary
        systemHealth={systemHealth}
        mqttConnection={mqttConnection}
        databaseConnection={databaseConnection}
        totalDevices={overview?.totalDevices ?? 0}
      />

      <DeviceStatusSummary
        onlineDevices={overview?.onlineDevices ?? count('ONLINE')}
        offlineDevices={overview?.offlineDevices ?? count('OFFLINE')}
        unknownDevices={overview?.unknownDevices ?? count('UNKNOWN')}
      />

      <Box className="overview__lower-grid">
        <DeviceTable devices={devices} />
        <LiveActivity activities={activities} />
      </Box>
    </Box>
  )
}