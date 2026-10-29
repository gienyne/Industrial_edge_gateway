import { useEffect, useMemo, useState } from 'react'
import {Alert, Box, CircularProgress, FormControl, InputAdornment, MenuItem, Select, TextField, Typography,} from '@mui/material'
import SearchOutlined from '@mui/icons-material/SearchOutlined'

import DeviceSummary from '../components/devices/DeviceSummary'
import DevicesTable from '../components/devices/DevicesTable'
import type { ApiDeviceSummary } from '../services/deviceService'
import { getDevices } from '../services/deviceService'

import '../styles/Devices.css'

type StatusFilter = 'ALL' | 'ONLINE' | 'OFFLINE' | 'UNKNOWN'

type DevicesProps = {
  onSelectDevice: (deviceId: string) => void
}

export default function Devices({ onSelectDevice }: DevicesProps) {
  const [devices, setDevices] = useState<ApiDeviceSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [apiError, setApiError] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')

  useEffect(() => {
    let cancelled = false
    let timeoutId: number | undefined

    async function loadDevices() {
      try {
        const data = await getDevices()

        if (!cancelled) {
          setDevices(data)
          setApiError(false)
        }
      } catch (error) {
        console.error('Unable to load devices from API:', error)

        if (!cancelled) {
          setApiError(true)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)

          timeoutId = window.setTimeout(() => {
            void loadDevices()
          }, 2000)
        }
      }
    }

    void loadDevices()

    return () => {
      cancelled = true

      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId)
      }
    }
  }, [])

  const totalDevices = devices.length

  const onlineDevices = devices.filter(
    (device) => device.status === 'ONLINE',
  ).length

  const offlineDevices = devices.filter(
    (device) => device.status === 'OFFLINE',
  ).length

  const filteredDevices = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase()

    return devices.filter((device) => {
      const matchesSearch = device.deviceId
        .toLowerCase()
        .includes(normalizedQuery)

      const matchesStatus =
        statusFilter === 'ALL' || device.status === statusFilter

      return matchesSearch && matchesStatus
    })
  }, [devices, searchQuery, statusFilter])

  return (
    <Box className="devices">
      {/* Title + search stay visible while the page scrolls. */}
      <Box className="devices__bar">
        <Box className="devices__intro">
          <Typography variant="h1" className="devices__title">
            Devices
          </Typography>

          <Typography variant="body2" color="text.secondary">
            Manage and monitor your connected devices.
          </Typography>
        </Box>

        <Box className="devices__toolbar">
          <TextField
            className="devices__search"
            size="small"
            placeholder="Search devices..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            slotProps={{
              htmlInput: { 'aria-label': 'Search devices' },
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchOutlined />
                  </InputAdornment>
                ),
              },
            }}
          />

          <FormControl className="devices__status-filter" size="small">
            <Select
              value={statusFilter}
              onChange={(event) =>
                setStatusFilter(event.target.value as StatusFilter)
              }
              inputProps={{ 'aria-label': 'Filter devices by status' }}
            >
              <MenuItem value="ALL">All Statuses</MenuItem>
              <MenuItem value="ONLINE">Online</MenuItem>
              <MenuItem value="OFFLINE">Offline</MenuItem>
              <MenuItem value="UNKNOWN">Unknown</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Box>

      {apiError && (
        <Alert severity="error" className="devices__error">
          Unable to load devices. Please try again.
        </Alert>
      )}

      {loading && devices.length === 0 ? (
        <Box className="devices__loading">
          <CircularProgress size={20} />
          <Typography variant="body2" color="text.secondary">
            Loading devices...
          </Typography>
        </Box>
      ) : (
        <>
          <DeviceSummary
            totalDevices={totalDevices}
            onlineDevices={onlineDevices}
            offlineDevices={offlineDevices}
          />

          <DevicesTable
            devices={filteredDevices}
            onSelectDevice={onSelectDevice}
          />
        </>
      )}
    </Box>
  )
}