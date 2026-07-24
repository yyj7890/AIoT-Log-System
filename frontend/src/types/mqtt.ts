export interface MqttStatus {
  enabled: boolean
  mode: 'lan' | 'remote'
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

export interface MqttRemoteCredentialStatus {
  username: string
  passwordConfigured: boolean
  runtimeOverrideEnabled: boolean
}
