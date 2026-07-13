export interface MqttStatus {
  enabled: boolean
  brokerUrl: string
  clientId: string
  topic: string
  logTopic: string
  qos: number
  connected: boolean
  lastConnectedAt?: string
  lastDisconnectedAt?: string
  lastMessageAt?: string
  lastMessageTopic?: string
  receivedCount: number
  handledCount: number
  failedCount: number
  lastError?: string
}

export interface MqttGlobalCredentialStatus {
  username: string
  passwordConfigured: boolean
  anonymousAccessEnabled: boolean
  activationPending: boolean
}
