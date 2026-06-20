<script setup>
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  points: {
    type: Array,
    default: () => []
  },
  metric: {
    type: String,
    required: true
  }
})

const chartElement = ref(null)
let chart = null
let resizeObserver = null

const metricConfig = computed(() => ({
  cpu: {
    field: 'cpuUsagePercent',
    label: 'CPU 使用率',
    color: '#0f766e',
    unit: '%',
    max: 100
  },
  memory: {
    field: 'memoryUsagePercent',
    label: '内存使用率',
    color: '#2563eb',
    unit: '%',
    max: 100
  },
  tcp: {
    field: 'tcpConnectionCount',
    label: 'TCP 连接数',
    color: '#b45309',
    unit: '',
    max: null
  }
}[props.metric]))

const ariaLabel = computed(() => `${metricConfig.value.label}历史趋势图，共 ${props.points.length} 个数据点`)

onMounted(async () => {
  await nextTick()
  chart = init(chartElement.value, null, { renderer: 'canvas', useDirtyRect: true })
  renderChart()
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartElement.value)
  } else {
    window.addEventListener('resize', resizeChart)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})

watch(
  () => [props.points, props.metric],
  () => renderChart(),
  { deep: true }
)

function resizeChart() {
  chart?.resize()
}

function renderChart() {
  if (!chart) {
    return
  }

  const config = metricConfig.value
  chart.setOption({
    animationDuration: 220,
    color: [config.color],
    grid: {
      left: 48,
      right: 22,
      top: 22,
      bottom: 42,
      containLabel: false
    },
    tooltip: {
      trigger: 'axis',
      renderMode: 'richText',
      formatter(parameters) {
        const parameter = parameters[0]
        const point = props.points[parameter?.dataIndex]
        return point
          ? `${formatTooltipTime(point.reportedAt)}\n${config.label}: ${formatValue(point[config.field], config.unit)}`
          : ''
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.points.map((point) => formatAxisTime(point.reportedAt)),
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#64748b',
        hideOverlap: true,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: config.max ?? undefined,
      splitNumber: 4,
      axisLabel: {
        color: '#64748b',
        formatter: `{value}${config.unit}`,
        fontSize: 11
      },
      splitLine: { lineStyle: { color: '#e2e8f0' } }
    },
    series: [
      {
        name: config.label,
        type: 'line',
        data: props.points.map((point) => Number(point[config.field] ?? 0)),
        showSymbol: props.points.length < 20,
        symbolSize: 6,
        smooth: 0.25,
        lineStyle: { width: 2.5 },
        areaStyle: { color: config.color, opacity: 0.1 },
        emphasis: { focus: 'series' }
      }
    ]
  }, true)
}

function formatAxisTime(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function formatTooltipTime(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false })
}

function formatValue(value, unit) {
  const numericValue = Number(value ?? 0)
  return `${unit ? numericValue.toFixed(1) : Math.round(numericValue)}${unit}`
}
</script>

<template>
  <div ref="chartElement" class="metric-history-chart" role="img" :aria-label="ariaLabel"></div>
</template>
