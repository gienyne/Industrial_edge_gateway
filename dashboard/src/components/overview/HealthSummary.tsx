import {Box, Card, CardContent, Typography,} from '@mui/material'

import MonitorHeartOutlined from '@mui/icons-material/MonitorHeartOutlined'
import LanOutlined from '@mui/icons-material/LanOutlined'
import StorageOutlined from '@mui/icons-material/StorageOutlined'
import DevicesOutlined from '@mui/icons-material/DevicesOutlined'

import type { ReactNode } from 'react'
import '../../styles/HealthSummary.css'

function formatStatus(value: string): string {
  if (!value || value === 'UNKNOWN') return 'Unknown'

  return value.charAt(0) + value.slice(1).toLowerCase()
}

function connectionVariant(
  value: string,
): SummaryCardProps['variant'] {
  if (value === 'CONNECTED') return 'success'
  if (value === 'DISCONNECTED') return 'error'

  return 'warning'
}

type HealthSummaryProps = {
  systemHealth: string
  mqttConnection: string
  databaseConnection: string
  totalDevices: number
}

type SummaryCardProps = {
  title: string
  value: string | number
  subtitle: string
  variant: 'success' | 'warning' | 'error' | 'primary'
  icon: ReactNode
}

function SummaryCard({
  title,
  value,
  subtitle,
  variant,
  icon,
}: SummaryCardProps) {
  return (
    <Card className="health-summary__card">
      <CardContent className="health-summary__card-content">
        <Box
          className="health-summary__icon"
          data-variant={variant}
        >
          {icon}
        </Box>

        <Box className="health-summary__details">
          <Typography
            variant="body2"
            color="text.secondary"
            className="health-summary__label"
          >
            {title}
          </Typography>

          <Typography
            variant="h6"
            className="health-summary__value"
          >
            {value}
          </Typography>

          <Typography
            variant="caption"
            color="text.secondary"
            className="health-summary__subtitle"
          >
            {subtitle}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  )
}

export default function HealthSummary({
  systemHealth,
  mqttConnection,
  databaseConnection,
  totalDevices,
}: HealthSummaryProps) {
  const cards: SummaryCardProps[] = [
    {
      title: 'System Health',
      value: formatStatus(systemHealth),
      subtitle: 'Backend service status',
      variant: systemHealth === 'HEALTHY' ? 'success' : 'warning',
      icon: <MonitorHeartOutlined />,
    },
    {
      title: 'MQTT Connection',
      value: formatStatus(mqttConnection),
      subtitle: 'MQTT broker connection',
      variant: connectionVariant(mqttConnection),
      icon: <LanOutlined />,
    },
    {
      title: 'Database',
      value: formatStatus(databaseConnection),
      subtitle: 'Database connection',
      variant: connectionVariant(databaseConnection),
      icon: <StorageOutlined />,
    },
    {
      title: 'Total Devices',
      value: totalDevices,
      subtitle: 'Registered devices',
      variant: 'primary',
      icon: <DevicesOutlined />,
    },
  ]

  return (
    <Box className="health-summary">
      {cards.map((card) => (
        <SummaryCard key={card.title} {...card} />
      ))}
    </Box>
  )
}
