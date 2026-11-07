import { Box, Paper, Typography } from '@mui/material'
import DevicesOutlined from '@mui/icons-material/DevicesOutlined'
import CheckCircle from '@mui/icons-material/CheckCircle'
import CancelOutlined from '@mui/icons-material/CancelOutlined'

type DeviceSummaryProps = {
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
}

export default function DeviceSummary({
  totalDevices,
  onlineDevices,
  offlineDevices,
}: DeviceSummaryProps) {
  const summaries = [
    {
      label: 'Total Devices',
      value: totalDevices,
      detail: 'Registered devices',
      icon: <DevicesOutlined />,
      className: 'device-summary__icon--total',
    },
    {
      label: 'Online',
      value: onlineDevices,
      detail: 'Currently connected',
      icon: <CheckCircle />,
      className: 'device-summary__icon--online',
    },
    {
      label: 'Offline',
      value: offlineDevices,
      detail: 'Currently disconnected',
      icon: <CancelOutlined />,
      className: 'device-summary__icon--offline',
    },
  ]

  return (
    <Box className="device-summary">
      {summaries.map((summary) => (
        <Paper
          key={summary.label}
          variant="outlined"
          className="device-summary__card"
        >
          <Box className={`device-summary__icon ${summary.className}`}>
            {summary.icon}
          </Box>

          <Box className="device-summary__content">
            <Typography
              variant="body2"
              className="device-summary__label"
            >
              {summary.label}
            </Typography>

            <Typography
              variant="h4"
              className="device-summary__value"
            >
              {summary.value}
            </Typography>

            <Typography variant="caption" color="text.secondary">
              {summary.detail}
            </Typography>
          </Box>
        </Paper>
      ))}
    </Box>
  )
}
