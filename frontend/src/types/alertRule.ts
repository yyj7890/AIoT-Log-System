export interface AlertRule {
  id: number
  name: string
  deviceId?: number
  deviceName?: string
  deviceCode?: string
  metric: string
  operator: string
  thresholdValue: number
  level: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface AlertRulePayload {
  name: string
  deviceId?: number
  metric: string
  operator: string
  thresholdValue: number
  level?: string
  enabled?: boolean
}

export interface AlertRuleQuery {
  deviceId?: number
  enabled?: boolean
}
