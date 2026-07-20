import { request } from './http'
import type { SystemRuntime } from '@/types/system'

export function getSystemRuntime() {
  return request<SystemRuntime>({
    url: '/system/runtime',
    method: 'GET',
    params: { _t: Date.now() },
    headers: { 'Cache-Control': 'no-cache' },
    silent: true
  })
}
