import { request } from './http'
import type { PageResult } from '@/types/api'
import type { LogPayload, LogQuery, LogRecord, LogStatus } from '@/types/log'

export function getLogList(params: LogQuery) {
  return request<PageResult<LogRecord>>({
    url: '/logs',
    method: 'GET',
    params: { ...params, _t: Date.now() },
    headers: { 'Cache-Control': 'no-cache' }
  })
}

export function getLogDetail(id: number, silent = false) {
  return request<LogRecord>({
    url: `/logs/${id}`,
    method: 'GET',
    params: { _t: Date.now() },
    headers: { 'Cache-Control': 'no-cache' },
    silent
  })
}

export function createLog(data: LogPayload) {
  return request<LogRecord>({ url: '/logs', method: 'POST', data })
}

export function updateLog(id: number, data: LogPayload) {
  return request<LogRecord>({ url: `/logs/${id}`, method: 'PUT', data })
}

export function updateLogStatus(id: number, status: LogStatus) {
  return request<LogRecord>({ url: `/logs/${id}/status`, method: 'PATCH', data: { status } })
}

export function deleteLog(id: number) {
  return request<void>({ url: `/logs/${id}`, method: 'DELETE' })
}

export function deleteLogs(ids: number[]) {
  return request<void>({ url: '/logs', method: 'DELETE', data: { ids } })
}
