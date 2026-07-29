export interface DeviceComponent {
  id: number
  deviceId: number
  name: string
  category?: string
  model?: string
  quantity: number
  notes?: string
  source: 'MANUAL' | 'DEVICE'
  updatedAt?: string
}

export interface DeviceComponentPayload {
  name: string
  category?: string
  model?: string
  quantity?: number
  notes?: string
}
