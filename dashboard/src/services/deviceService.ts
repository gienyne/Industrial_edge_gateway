import { apiFetch } from './apiClient'

export type DeviceStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN'

export interface ApiDeviceSummary {
  deviceId: string
  status: DeviceStatus
  lastSeen: string | null
  metricCount: number
}

export interface ApiMeasurement {
  timestamp: string
  metricName: string
  dataType: string
  value: boolean | number | string | null
  unit: string | null
}

export interface ApiDeviceDetails {
  deviceId: string
  status: DeviceStatus
  groupId: string | null
  edgeNodeId: string | null
  lastSeen: string | null
  currentValues: ApiMeasurement[]
}

export function getDevices(): Promise<ApiDeviceSummary[]> {
  return apiFetch<ApiDeviceSummary[]>('/devices')
}

export function getDevice(
  deviceId: string,
): Promise<ApiDeviceDetails> {
  return apiFetch<ApiDeviceDetails>(
    `/devices/${encodeURIComponent(deviceId)}`,
  )
}

export function getRecentMeasurements(
  deviceId: string,
): Promise<ApiMeasurement[]> {
  return apiFetch<ApiMeasurement[]>(
    `/devices/${encodeURIComponent(deviceId)}/measurements/recent`,
  )
}
