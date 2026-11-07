import { Box, Paper, Typography } from '@mui/material'
import MemoryOutlined from '@mui/icons-material/MemoryOutlined'
import BarChartOutlined from '@mui/icons-material/BarChartOutlined'

import StatusChip from '../StatusChip'
import type { DeviceStatus } from '../StatusChip'

import '../../styles/DeviceTable.css'

export type OverviewDevice = {
  deviceId: string
  status: DeviceStatus
  lastSeen: string | null
  metricCount: number
}

type DeviceTableProps = {
  devices: OverviewDevice[]
  onSelectDevice?: (deviceId: string) => void
}

function formatLastSeen(value: string | null): string {
  if (!value) return 'Never'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}

export default function DeviceTable({
  devices,
  onSelectDevice,
}: DeviceTableProps) {
  return (
    <Paper variant="outlined" className="device-table">
      <Box className="device-table__header">
        <Typography variant="h6" className="device-table__title">
          Devices
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Registered devices and their current status
        </Typography>
      </Box>

      {/* The only scrolling area of the card: vertical scroll from the 4th device on. */}
      <Box className="device-table__container" role="table" aria-label="Devices">
        <Box className="device-table__head" role="row">
          <span role="columnheader">Device</span>
          <span role="columnheader">Status</span>
          <span role="columnheader">Last Seen</span>
          <span role="columnheader" className="device-table__head-right">
            Metrics
          </span>
        </Box>

        {devices.length === 0 ? (
          <Box className="device-table__empty">
            <Typography variant="body2" color="text.secondary">
              No devices found
            </Typography>
          </Box>
        ) : (
          devices.map((device) => (
            <Box
              key={device.deviceId}
              role="row"
              className={
                onSelectDevice
                  ? 'device-table__row device-table__row--selectable'
                  : 'device-table__row'
              }
              data-status={device.status.toLowerCase()}
              onClick={
                onSelectDevice
                  ? () => onSelectDevice(device.deviceId)
                  : undefined
              }
            >
              <Box className="device-table__device" role="cell">
                <Box className="device-table__icon">
                  <MemoryOutlined />
                </Box>

                <Typography
                  variant="body2"
                  className="device-table__device-name"
                  title={device.deviceId}
                >
                  {device.deviceId}
                </Typography>
              </Box>

              <Box role="cell">
                <StatusChip status={device.status} />
              </Box>

              <Box role="cell">
                <Typography
                  variant="body2"
                  color="text.secondary"
                  className="device-table__last-seen"
                >
                  {formatLastSeen(device.lastSeen)}
                </Typography>
              </Box>

              <Box role="cell" className="device-table__metrics-cell">
                <span className="device-table__metrics-pill">
                  <BarChartOutlined />
                  {device.metricCount}
                </span>
              </Box>
            </Box>
          ))
        )}
      </Box>
    </Paper>
  )
}