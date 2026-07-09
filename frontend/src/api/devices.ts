import { request } from './http'
import type { PageResult } from '@/types/api'
import type { Device, DevicePayload, DeviceQuery } from '@/types/device'

export function getDeviceList(params: DeviceQuery) {
  return request<PageResult<Device>>({ url: '/devices', method: 'GET', params })
}

export function getDeviceDetail(id: number) {
  return request<Device>({ url: `/devices/${id}`, method: 'GET' })
}

export function createDevice(data: DevicePayload) {
  return request<Device>({ url: '/devices', method: 'POST', data })
}

export function updateDevice(id: number, data: DevicePayload) {
  return request<Device>({ url: `/devices/${id}`, method: 'PUT', data })
}

export function deleteDevice(id: number) {
  return request<void>({ url: `/devices/${id}`, method: 'DELETE' })
}
