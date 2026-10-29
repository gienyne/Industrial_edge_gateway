import {Box, IconButton, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography,} from '@mui/material'
import MemoryOutlined from '@mui/icons-material/MemoryOutlined'
import BarChartOutlined from '@mui/icons-material/BarChartOutlined'
import ChevronRight from '@mui/icons-material/ChevronRight'

import StatusChip from '../StatusChip'
import type { ApiDeviceSummary } from '../../services/deviceService'

import '../../styles/Devices.css'

type DevicesTableProps = {
  devices: ApiDeviceSummary[]
  onSelectDevice: (deviceId: string) => void
}

function formatLastSeen(value: string | null): string {
  if (!value) return 'Never'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export default function DevicesTable({
  devices,
  onSelectDevice,
}: DevicesTableProps) {
  return (
    <Paper variant="outlined" className="devices-table">
      <Box className="devices-table__header">
        <Typography variant="h6" className="devices-table__title">
          Devices ({devices.length})
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Registered devices and their current status
        </Typography>
      </Box>

      {/* Vertical scroll happens inside this box */}
      <TableContainer className="devices-table__container">
        <Table stickyHeader aria-label="Registered devices">
          <TableHead>
            <TableRow>
              <TableCell className="devices-table__col-device">
                Device
              </TableCell>
              <TableCell className="devices-table__col-status">
                Status
              </TableCell>
              <TableCell className="devices-table__col-seen">
                Last Seen
              </TableCell>
              <TableCell className="devices-table__col-metrics">
                Metrics
              </TableCell>
              <TableCell
                className="devices-table__col-action"
                aria-label="Open details"
              />
            </TableRow>
          </TableHead>

          <TableBody>
            {devices.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={5}
                  align="center"
                  className="devices-table__empty"
                >
                  <Typography variant="body2" color="text.secondary">
                    No devices found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              devices.map((device) => (
                <TableRow
                  key={device.deviceId}
                  hover
                  onClick={() => onSelectDevice(device.deviceId)}
                  className="devices-table__row"
                  tabIndex={0}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      onSelectDevice(device.deviceId)
                    }
                  }}
                >
                  <TableCell className="devices-table__device-cell">
                    <Box className="devices-table__device">
                      <Box className="devices-table__device-icon">
                        <MemoryOutlined />
                      </Box>

                      <Box className="devices-table__device-info">
                        <Typography
                          variant="body2"
                          className="devices-table__device-name"
                        >
                          {device.deviceId}
                        </Typography>

                        <Typography
                          variant="caption"
                          color="text.secondary"
                        >
                          Industrial device
                        </Typography>
                      </Box>
                    </Box>
                  </TableCell>

                  <TableCell>
                    <StatusChip status={device.status} />
                  </TableCell>

                  <TableCell className="devices-table__last-seen">
                    {formatLastSeen(device.lastSeen)}
                  </TableCell>

                  <TableCell>
                    <Box className="devices-table__metrics">
                      <Box className="devices-table__metrics-icon">
                        <BarChartOutlined />
                      </Box>

                      <Typography
                        variant="body2"
                        className="devices-table__metric-count"
                      >
                        {device.metricCount}
                        <span className="devices-table__metric-label">
                          {device.metricCount === 1 ? ' metric' : ' metrics'}
                        </span>
                      </Typography>
                    </Box>
                  </TableCell>

                  <TableCell align="right">
                    <IconButton
                      size="small"
                      aria-label={`Open ${device.deviceId}`}
                      className="devices-table__chevron"
                      onClick={(event) => {
                        event.stopPropagation()
                        onSelectDevice(device.deviceId)
                      }}
                    >
                      <ChevronRight />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  )
}