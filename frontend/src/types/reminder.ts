export type ReminderStatus = 'SCHEDULED' | 'TRIGGERING' | 'PUBLISHED' | 'FAILED' | 'CANCELED'

export interface Reminder {
  id: number
  requestId: string
  deviceCode: string
  message: string
  remindAt: string
  status: ReminderStatus
  deliveryTaskId?: string
  triggeredAt?: string
}
