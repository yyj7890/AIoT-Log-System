import { request } from './http'
import type { PageResult } from '@/types/api'
import type { DeviceReport, DeviceReportPayload, DeviceReportQuery } from '@/types/report'

export function getDeviceReportList(params: DeviceReportQuery) {
  return request<PageResult<DeviceReport>>({ url: '/device-reports', method: 'GET', params })
}

export function createDeviceReport(data: DeviceReportPayload) {
  return request<DeviceReport>({ url: '/device-reports', method: 'POST', data })
}
