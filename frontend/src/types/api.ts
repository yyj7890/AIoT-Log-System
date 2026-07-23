export interface ApiResponse<T> {
  code: number
  message: string
  errorCode?: string
  traceId?: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface EnumOption {
  label: string
  value: string
}

export interface EnumMap {
  deviceStatus: EnumOption[]
  logType: EnumOption[]
  logLevel: EnumOption[]
  logStatus: EnumOption[]
  logSource: EnumOption[]
  alertMetric: EnumOption[]
  alertOperator: EnumOption[]
}
