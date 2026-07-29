import type { LogRecord } from './log'
import type { DeviceReport } from './report'

export type DeviceStatus = 'NORMAL' | 'ABNORMAL' | 'OFFLINE' | 'MAINTENANCE'
export type DeviceMonitoringMode = 'LOG_ONLY' | 'TELEMETRY'

export interface Device {
  id: number
  name: string
  deviceCode: string
  type: string
  manufacturer?: string
  model?: string
  serialNumber?: string
  firmwareVersion?: string
  monitoringMode: DeviceMonitoringMode
  location?: string
  status: DeviceStatus
  description?: string
  lastOnlineAt?: string
  createdAt: string
  updatedAt: string
  logCount?: number
  errorLogCount?: number
  pendingLogCount?: number
  recentLogs?: LogRecord[]
  recentReports?: DeviceReport[]
}

export interface DevicePayload {
  name: string
  deviceCode?: string
  type: string
  manufacturer?: string
  model?: string
  serialNumber?: string
  firmwareVersion?: string
  monitoringMode: DeviceMonitoringMode
  location?: string
  status: DeviceStatus
  description?: string
}

export interface DeviceQuery {
  page?: number
  pageSize?: number
  keyword?: string
  type?: string
  status?: string
}
