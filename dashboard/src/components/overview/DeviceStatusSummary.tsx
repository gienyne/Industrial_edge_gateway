import { Box, Card, CardContent, Typography } from '@mui/material'

import '../../styles/DeviceStatusSummary.css'

type DeviceStatusSummaryProps = {
  onlineDevices: number
  offlineDevices: number
  unknownDevices: number
}

type StatusCardProps = {
  label: string
  count: number
  color: 'online' | 'offline' | 'unknown'
  description: string
}

function StatusCard({
  label,
  count,
  color,
  description,
}: StatusCardProps) {
  return (
    <Card className="device-status-summary__card">
      <CardContent className="device-status-summary__content">
        <Box className="device-status-summary__heading">
          <Box
            className="device-status-summary__indicator"
            data-status={color}
          />

          <Typography variant="body2" color="text.secondary">
            {label}
          </Typography>
        </Box>

        <Typography
          variant="h4"
          className="device-status-summary__count"
        >
          {count}
        </Typography>

        <Typography
          variant="caption"
          color="text.secondary"
          className="device-status-summary__description"
        >
          {description}
        </Typography>
      </CardContent>
    </Card>
  )
}

export default function DeviceStatusSummary({
  onlineDevices,
  offlineDevices,
  unknownDevices,
}: DeviceStatusSummaryProps) {
  const statuses: StatusCardProps[] = [
    {
      label: 'Online',
      count: onlineDevices,
      color: 'online',
      description: 'Currently connected',
    },
    {
      label: 'Offline',
      count: offlineDevices,
      color: 'offline',
      description: 'Not responding',
    },
    {
      label: 'Unknown',
      count: unknownDevices,
      color: 'unknown',
      description: 'Status not confirmed',
    },
  ]

  return (
    <Box className="device-status-summary">
      {statuses.map((status) => (
        <StatusCard key={status.label} {...status} />
      ))}
    </Box>
  )
}