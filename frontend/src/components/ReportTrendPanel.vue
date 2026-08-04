<template>
  <div class="trend-grid">
    <div v-for="metric in availableMetrics" :key="metric.key" class="trend-item">
      <div class="trend-head">
        <span class="trend-name">{{ metric.name }}</span>
        <span class="trend-current">{{ latestValue(metric.key, metric.unit) }}</span>
      </div>
      <svg class="trend-chart" viewBox="0 0 240 72" preserveAspectRatio="none" role="img">
        <polyline v-if="buildPoints(metric.key)" class="trend-line" :points="buildPoints(metric.key)" />
        <line class="trend-axis" x1="0" y1="60" x2="240" y2="60" />
      </svg>
      <div class="trend-foot">
        <span>{{ rangeLabel(metric.key, 'min') }}</span>
        <span>{{ rangeLabel(metric.key, 'max') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DeviceReport } from '@/types/report'

type MetricKey = 'temperature' | 'humidity' | 'pressure' | 'illuminance' | 'voltage' | 'signalStrength'

const props = defineProps<{
  reports: DeviceReport[]
}>()

const metrics: Array<{ key: MetricKey; name: string; unit: string }> = [
  { key: 'temperature', name: '温度', unit: '℃' },
  { key: 'humidity', name: '湿度', unit: '%' },
  { key: 'pressure', name: '气压', unit: 'hPa' },
  { key: 'illuminance', name: '光照', unit: 'lux' },
  { key: 'voltage', name: '电压', unit: 'V' },
  { key: 'signalStrength', name: '信号', unit: 'dBm' }
]

const availableMetrics = computed(() => metrics.filter((metric) => values(metric.key).length > 0))

function values(key: MetricKey) {
  return props.reports
    .slice()
    .reverse()
    .map((report) => report[key])
    .filter((value): value is number => value !== undefined && value !== null)
}

function buildPoints(key: MetricKey) {
  const list = values(key)
  if (list.length === 0) return ''
  if (list.length === 1) return `0,36 240,36`

  const min = Math.min(...list)
  const max = Math.max(...list)
  const span = max - min || 1
  return list
    .map((value, index) => {
      const x = (index / (list.length - 1)) * 240
      const y = 60 - ((value - min) / span) * 48
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

function latestValue(key: MetricKey, unit: string) {
  const latest = props.reports.find((report) => report[key] !== undefined && report[key] !== null)
  return latest ? `${latest[key]}${unit}` : '-'
}

function rangeLabel(key: MetricKey, type: 'min' | 'max') {
  const list = values(key)
  if (list.length === 0) return '-'
  const value = type === 'min' ? Math.min(...list) : Math.max(...list)
  return `${type === 'min' ? '低' : '高'} ${value}`
}
</script>

<style scoped>
.trend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 12px;
}

.trend-item {
  min-width: 0;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.trend-head,
.trend-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.trend-name {
  color: var(--app-muted);
  font-size: 13px;
}

.trend-current {
  font-weight: 700;
}

.trend-chart {
  display: block;
  width: 100%;
  height: 72px;
  margin-top: 8px;
}

.trend-line {
  fill: none;
  stroke: #2f855a;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.trend-axis {
  stroke: #d9dee8;
  stroke-width: 1;
}

.trend-foot {
  color: var(--app-muted);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .trend-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 720px) {
  .trend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
