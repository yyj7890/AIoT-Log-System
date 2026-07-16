import { request } from './http'
import type { MqttGlobalCredentialStatus, MqttStatus } from '@/types/mqtt'

export function getMqttStatus() {
  return request<MqttStatus>({
    url: '/mqtt/status',
    method: 'GET',
    params: { _t: Date.now() },
    headers: { 'Cache-Control': 'no-cache' }
  })
}

export function getMqttGlobalCredential() {
  return request<MqttGlobalCredentialStatus>({ url: '/mqtt/global-credential', method: 'GET' })
}

export function saveMqttGlobalCredential(data: { username: string; password: string }) {
  return request<MqttGlobalCredentialStatus>({ url: '/mqtt/global-credential', method: 'PUT', data })
}

export function setMqttAuthentication(enabled: boolean) {
  return request<MqttGlobalCredentialStatus>({ url: '/mqtt/global-credential/authentication', method: 'PUT', params: { enabled } })
}
