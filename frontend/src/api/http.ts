import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'

type RequestConfig = AxiosRequestConfig & {
  silent?: boolean
}

export class ApiRequestError extends Error {
  code?: number

  constructor(message: string, code?: number) {
    super(message)
    this.name = 'ApiRequestError'
    this.code = code
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResponse<unknown>
    if (typeof result?.code === 'number' && result.code !== 200) {
      const message = result.message || '请求失败'
      if (!(response.config as RequestConfig).silent) {
        ElMessage.error(message)
      }
      return Promise.reject(new ApiRequestError(message, result.code))
    }
    return response
  },
  (error) => {
    const message = error?.response?.data?.message || error.message || '网络请求失败'
    if (!(error?.config as RequestConfig | undefined)?.silent) {
      ElMessage.error(message)
    }
    return Promise.reject(new ApiRequestError(message, error?.response?.status))
  }
)

export async function request<T>(config: RequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}
