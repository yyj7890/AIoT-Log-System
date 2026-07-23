import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'

type RequestConfig = AxiosRequestConfig & {
  silent?: boolean
}

export class ApiRequestError extends Error {
  code?: number
  errorCode?: string
  traceId?: string

  constructor(message: string, code?: number, errorCode?: string, traceId?: string) {
    super(message)
    this.name = 'ApiRequestError'
    this.code = code
    this.errorCode = errorCode
    this.traceId = traceId
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
      return Promise.reject(new ApiRequestError(message, result.code, result.errorCode, result.traceId))
    }
    return response
  },
  (error) => {
    const result = error?.response?.data as ApiResponse<unknown> | undefined
    const message = result?.message || error.message || '网络请求失败'
    if (!(error?.config as RequestConfig | undefined)?.silent) {
      ElMessage.error(message)
    }
    return Promise.reject(new ApiRequestError(
      message,
      error?.response?.status,
      result?.errorCode,
      result?.traceId
    ))
  }
)

export async function request<T>(config: RequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}
