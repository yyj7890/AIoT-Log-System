import { request } from './http'
import type { EnumMap } from '@/types/api'

export function getEnums() {
  return request<EnumMap>({ url: '/enums', method: 'GET' })
}
