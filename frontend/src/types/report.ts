import type { DeviceStatus } from './device'

export interface DeviceReport {
  id: number
  deviceId: number
  deviceName?: string
  deviceCode?: string
  temperature?: number
  humidity?: number
  voltage?: number
  signalStrength?: number
  status: DeviceStatus
  message?: string
  abnormal: boolean
  generatedLogId?: number
  reportedAt: string
  createdAt: string
}

export interface DeviceReportPayload {
  deviceCode: string
  temperature?: number
  humidity?: number
  voltage?: number
  signalStrength?: number
  status?: DeviceStatus
  message?: string
  reportedAt?: string
}

export interface DeviceReportQuery {
  page?: number
  pageSize?: number
  deviceId?: number
  status?: string
}
