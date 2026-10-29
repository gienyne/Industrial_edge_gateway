import type { ReactNode } from 'react'
import SensorsOutlined from '@mui/icons-material/SensorsOutlined'
import SensorsOffOutlined from '@mui/icons-material/SensorsOffOutlined'
import RouterOutlined from '@mui/icons-material/RouterOutlined'
import AutorenewOutlined from '@mui/icons-material/AutorenewOutlined'
import SyncProblemOutlined from '@mui/icons-material/SyncProblemOutlined'
import InfoOutlined from '@mui/icons-material/InfoOutlined'

import type { ApiEventItem, EventSeverity, EventType } from '../../types/event'

export const EVENT_TYPE_OPTIONS: { value: EventType; label: string }[] = [
  { value: 'DEVICE_ONLINE', label: 'Device online' },
  { value: 'DEVICE_OFFLINE', label: 'Device offline' },
  { value: 'EDGE_NODE_ONLINE', label: 'Edge node online' },
  { value: 'EDGE_NODE_OFFLINE', label: 'Edge node offline' },
  { value: 'REBIRTH_REQUESTED', label: 'Rebirth requested' },
  { value: 'SEQUENCE_GAP', label: 'Sequence gap' },
]

export const SEVERITY_OPTIONS: { value: EventSeverity; label: string }[] = [
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'INFO', label: 'Info' },
]

export const SEVERITY_LABEL: Record<EventSeverity, string> = {
  HIGH: 'High',
  MEDIUM: 'Medium',
  INFO: 'Info',
}

/* Icon and name of an event type. */
export function getEventVisual(type: EventType): {
  icon: ReactNode
  label: string
} {
  switch (type) {
    case 'DEVICE_ONLINE':
      return { icon: <SensorsOutlined />, label: 'Device online' }
    case 'DEVICE_OFFLINE':
      return { icon: <SensorsOffOutlined />, label: 'Device offline' }
    case 'EDGE_NODE_ONLINE':
      return { icon: <RouterOutlined />, label: 'Edge node online' }
    case 'EDGE_NODE_OFFLINE':
      return { icon: <RouterOutlined />, label: 'Edge node offline' }
    case 'REBIRTH_REQUESTED':
      return { icon: <AutorenewOutlined />, label: 'Rebirth requested' }
    case 'SEQUENCE_GAP':
      return { icon: <SyncProblemOutlined />, label: 'Sequence gap' }
    default:
      return { icon: <InfoOutlined />, label: String(type) }
  }
}

/* Returns the event severity, defaulting to "INFO" if it is missing. */
export function getSeverity(event: ApiEventItem): EventSeverity {
  return event.severity ?? 'INFO'
}