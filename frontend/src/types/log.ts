import type { Tag } from './tag'

export type LogType = 'RUNNING' | 'ERROR' | 'MAINTENANCE' | 'INSPECTION'
export type LogLevel = 'INFO' | 'WARNING' | 'ERROR'
export type LogStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED'
export type LogSource = 'MANUAL' | 'DEVICE' | 'SYSTEM'

export interface LogRecord {
  id: number
  deviceId: number
  deviceName: string
  deviceCode: string
  title: string
  content: string
  logType: LogType
  level: LogLevel
  status: LogStatus
  source: LogSource
  tags: Tag[]
  createdAt: string
  updatedAt: string
}

export interface LogPayload {
  deviceId: number
  title: string
  content: string
  logType: LogType
  level: LogLevel
  status: LogStatus
  tagIds?: number[]
}

export interface LogQuery {
  page?: number
  pageSize?: number
  deviceId?: number
  logType?: string
  level?: string
  status?: string
  source?: string
  tagId?: number
  keyword?: string
  startTime?: string
  endTime?: string
}
