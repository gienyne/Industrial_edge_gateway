export type EventSeverity = 'HIGH' | 'MEDIUM' | 'INFO'

export type EventType =
  | 'EDGE_NODE_ONLINE'
  | 'EDGE_NODE_OFFLINE'
  | 'DEVICE_ONLINE'
  | 'DEVICE_OFFLINE'
  | 'REBIRTH_REQUESTED'
  | 'SEQUENCE_GAP'

export interface ApiEventItem {
  id: number
  timestamp: string
  type: EventType
  severity: EventSeverity
  groupId: string | null
  edgeNodeId: string | null
  deviceId: string | null
  message: string
}

export interface ApiEventPage {
  events: ApiEventItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}