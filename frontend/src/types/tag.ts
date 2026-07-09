export interface Tag {
  id: number
  name: string
  color?: string
  createdAt?: string
  updatedAt?: string
}

export interface TagCreatePayload {
  name: string
  color?: string
}
