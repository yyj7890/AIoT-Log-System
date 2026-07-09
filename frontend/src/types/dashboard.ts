import type { LogRecord } from './log'

export interface DashboardSummary {
  deviceTotal: number
  normalDeviceCount: number
  abnormalDeviceCount: number
  offlineDeviceCount: number
  maintenanceDeviceCount: number
  pendingLogCount: number
  recentErrorLogs: LogRecord[]
  recentMaintenanceLogs: LogRecord[]
}
