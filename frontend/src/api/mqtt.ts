import { request } from './http'
import type { MqttStatus } from '@/types/mqtt'

export function getMqttStatus() {
  return request<MqttStatus>({ url: '/mqtt/status', method: 'GET' })
}
