import { Box, Card, CardContent, Typography } from '@mui/material'

import SensorsOutlined from '@mui/icons-material/SensorsOutlined'
import SensorsOffOutlined from '@mui/icons-material/SensorsOffOutlined'
import RouterOutlined from '@mui/icons-material/RouterOutlined'
import AutorenewOutlined from '@mui/icons-material/AutorenewOutlined'
import SyncProblemOutlined from '@mui/icons-material/SyncProblemOutlined'
import WarningAmberOutlined from '@mui/icons-material/WarningAmberOutlined'
import InfoOutlined from '@mui/icons-material/InfoOutlined'

import type { ReactNode } from 'react'
import '../../styles/LiveActivity.css'

export type ApiEvent = {
  id: number
  timestamp: string
  type: string
  groupId: string | null
  edgeNodeId: string | null
  deviceId: string | null
  message: string
}

type LiveActivityProps = {
  activities: ApiEvent[]
}

type EventStyle = {
  color: 'error' | 'warning' | 'success' | 'info' | 'default'
  icon: ReactNode
}

function formatTimestamp(timestamp: string): string {
  const date = new Date(timestamp)

  if (Number.isNaN(date.getTime())) return 'Invalid date'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

/*
 * Event types sent by the backend:
 * EDGE_NODE_ONLINE, EDGE_NODE_OFFLINE, DEVICE_ONLINE, DEVICE_OFFLINE,
 * REBIRTH_REQUESTED.
 * OFFLINE is checked before ONLINE so the order of the checks stays safe.
 */
function getEventStyle(type: string): EventStyle {
  const normalizedType = type.toUpperCase()
  const isEdgeNode = normalizedType.startsWith('EDGE_NODE')

  if (
    normalizedType.includes('OFFLINE') ||
    normalizedType.includes('DISCONNECT')
  ) {
    return {
      color: 'error',
      icon: isEdgeNode ? <RouterOutlined /> : <SensorsOffOutlined />,
    }
  }

  if (
    normalizedType.includes('ERROR') ||
    normalizedType.includes('FAIL')
  ) {
    return { color: 'error', icon: <WarningAmberOutlined /> }
  }

  if (normalizedType.includes('SEQUENCE')) {
    return { color: 'warning', icon: <SyncProblemOutlined /> }
  }

  if (normalizedType.includes('REBIRTH')) {
    return { color: 'warning', icon: <AutorenewOutlined /> }
  }

  if (normalizedType.includes('WARN')) {
    return { color: 'warning', icon: <WarningAmberOutlined /> }
  }

  if (
    normalizedType.includes('ONLINE') ||
    normalizedType.includes('CONNECT')
  ) {
    return {
      color: 'success',
      icon: isEdgeNode ? <RouterOutlined /> : <SensorsOutlined />,
    }
  }

  if (
    normalizedType.includes('DEVICE') ||
    normalizedType.includes('MESSAGE')
  ) {
    return { color: 'info', icon: <SensorsOutlined /> }
  }

  return { color: 'default', icon: <InfoOutlined /> }
}

export default function LiveActivity({
  activities,
}: LiveActivityProps) {
  return (
    <Card variant="outlined" className="live-activity">
      <CardContent className="live-activity__content">
        <Typography variant="h6" className="live-activity__title">
          Live Activity
        </Typography>

        <Typography
          variant="body2"
          color="text.secondary"
          className="live-activity__subtitle"
        >
          Recent system events
        </Typography>

        {activities.length === 0 ? (
          <Box className="live-activity__empty">
            <Typography variant="body2" color="text.secondary">
              No recent activity
            </Typography>
          </Box>
        ) : (
          <Box className="live-activity__list">
            {activities.map((activity) => {
              const style = getEventStyle(activity.type)

              return (
                <Box
                  key={activity.id}
                  className="live-activity__item"
                  data-variant={style.color}
                >
                  <Box className="live-activity__icon">
                    {style.icon}
                  </Box>

                  <Box className="live-activity__details">
                    <Typography
                      variant="body2"
                      className="live-activity__message"
                    >
                      {activity.message || activity.type}
                    </Typography>

                    <Typography
                      variant="caption"
                      color="text.secondary"
                      className="live-activity__metadata"
                    >
                      <span className="live-activity__source">
                        {activity.deviceId ??
                          activity.edgeNodeId ??
                          'System'}
                      </span>
                      {' · '}
                      {formatTimestamp(activity.timestamp)}
                    </Typography>
                  </Box>
                </Box>
              )
            })}
          </Box>
        )}
      </CardContent>
    </Card>
  )
}