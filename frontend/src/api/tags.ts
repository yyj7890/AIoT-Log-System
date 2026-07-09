import { request } from './http'
import type { Tag, TagCreatePayload } from '@/types/tag'

export function getTagList() {
  return request<Tag[]>({ url: '/tags', method: 'GET' })
}

export function createTag(data: TagCreatePayload) {
  return request<Tag>({ url: '/tags', method: 'POST', data })
}

export function deleteTag(id: number) {
  return request<void>({ url: `/tags/${id}`, method: 'DELETE' })
}
