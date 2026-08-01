import { request } from './http'
import type { EnvironmentSpace, EnvironmentSpacePayload, EnvironmentOutdoorReading, EnvironmentAnnouncementRule } from '@/types/environment'
export const getEnvironmentSpaces = () => request<EnvironmentSpace[]>({ url: '/environment-spaces', method: 'GET' })
export const createEnvironmentSpace = (data: EnvironmentSpacePayload) => request<EnvironmentSpace>({ url: '/environment-spaces', method: 'POST', data })
export const updateEnvironmentSpace = (id:number, data: EnvironmentSpacePayload) => request<EnvironmentSpace>({ url: `/environment-spaces/${id}`, method: 'PUT', data })
export const deleteEnvironmentSpace = (id: number) => request<void>({ url: `/environment-spaces/${id}`, method: 'DELETE' })
export const getEnvironmentOutdoor = (id: number) => request<EnvironmentOutdoorReading | null>({ url: `/environment-spaces/${id}/outdoor`, method: 'GET' })
export const refreshEnvironmentOutdoor = (id: number) => request<EnvironmentOutdoorReading>({ url: `/environment-spaces/${id}/outdoor/refresh`, method: 'POST' })
export const getEnvironmentAnnouncementRules = (spaceId?:number) => request<EnvironmentAnnouncementRule[]>({ url: '/environment-announcement-rules', method: 'GET', params: spaceId ? { spaceId } : undefined })
export const createEnvironmentAnnouncementRule = (data: Omit<EnvironmentAnnouncementRule,'id'>) => request<EnvironmentAnnouncementRule>({ url: '/environment-announcement-rules', method: 'POST', data })
export const deleteEnvironmentAnnouncementRule = (id:number) => request<void>({ url: `/environment-announcement-rules/${id}`, method: 'DELETE' })
