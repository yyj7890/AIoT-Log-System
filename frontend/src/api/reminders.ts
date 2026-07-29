import { request } from './http'
import type { Reminder } from '@/types/reminder'

export function getReminderList(deviceCode?: string) {
  return request<Reminder[]>({ url: '/reminders', method: 'GET', params: deviceCode ? { deviceCode } : undefined })
}

export function cancelReminder(id: number) {
  return request<void>({ url: `/reminders/${id}`, method: 'DELETE' })
}
