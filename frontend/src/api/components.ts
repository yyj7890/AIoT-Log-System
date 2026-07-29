import { request } from './http'
import type { DeviceComponent, DeviceComponentPayload } from '@/types/component'

export const getDeviceComponents = (deviceId: number) => request<DeviceComponent[]>({ url: `/devices/${deviceId}/components`, method: 'GET' })
export const createDeviceComponent = (deviceId: number, data: DeviceComponentPayload) => request<DeviceComponent>({ url: `/devices/${deviceId}/components`, method: 'POST', data })
export const updateDeviceComponent = (deviceId: number, id: number, data: DeviceComponentPayload) => request<DeviceComponent>({ url: `/devices/${deviceId}/components/${id}`, method: 'PUT', data })
export const deleteDeviceComponent = (deviceId: number, id: number) => request<void>({ url: `/devices/${deviceId}/components/${id}`, method: 'DELETE' })
