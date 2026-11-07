import type { ApiEventItem } from './event'

export type { EventType } from './event'

export type SystemHealth = 'HEALTHY' | 'DEGRADED'

export type ConnectionStatus = 'CONNECTED' | 'DISCONNECTED'

export type EntityStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN'

export interface ApiDeviceSummary {
  deviceId: string
  status: EntityStatus
  lastSeen: string | null
  metricCount: number
}

export type ApiEvent = ApiEventItem

export interface ApiOverviewResponse {
  systemHealth: SystemHealth
  mqttConnection: ConnectionStatus
  databaseConnection: ConnectionStatus
  synchronizing: boolean
  totalDevices: number
  onlineDevices: number
  offlineDevices: number
  unknownDevices: number
  devices: ApiDeviceSummary[]
  recentActivities: ApiEvent[]
}