import { request } from './http'
import type { AlertRule, AlertRulePayload, AlertRuleQuery } from '@/types/alertRule'

export function getAlertRules(params?: AlertRuleQuery) {
  return request<AlertRule[]>({ url: '/alert-rules', method: 'GET', params })
}

export function createAlertRule(data: AlertRulePayload) {
  return request<AlertRule>({ url: '/alert-rules', method: 'POST', data })
}

export function updateAlertRule(id: number, data: AlertRulePayload) {
  return request<AlertRule>({ url: `/alert-rules/${id}`, method: 'PUT', data })
}

export function deleteAlertRule(id: number) {
  return request<void>({ url: `/alert-rules/${id}`, method: 'DELETE' })
}
