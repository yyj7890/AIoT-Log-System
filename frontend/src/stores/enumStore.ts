import { defineStore } from 'pinia'
import { getEnums } from '@/api/enums'
import type { EnumMap, EnumOption } from '@/types/api'

const emptyEnums: EnumMap = {
  deviceStatus: [],
  logType: [],
  logLevel: [],
  logStatus: [],
  logSource: [],
  alertMetric: [],
  alertOperator: []
}

export const useEnumStore = defineStore('enumStore', {
  state: () => ({
    enums: { ...emptyEnums },
    loaded: false
  }),
  getters: {
    label:
      (state) =>
      (group: keyof EnumMap, value?: string): string => {
        if (!value) return '-'
        return state.enums[group].find((item: EnumOption) => item.value === value)?.label || value
      }
  },
  actions: {
    async loadEnums() {
      if (this.loaded) return
      this.enums = await getEnums()
      this.loaded = true
    }
  }
})
