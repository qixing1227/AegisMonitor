<script setup>
import {
  Activity,
  ArrowLeft,
  Cpu,
  HardDrive,
  Monitor,
  Network,
  RefreshCw,
  Server,
  ShieldCheck,
  Signal,
  TriangleAlert
} from 'lucide-vue-next'
import { defineAsyncComponent, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { dashboardStore as store } from './dashboardStore.js'
import { createRefreshLoop } from './refreshLoop.js'

const HostMetricChart = defineAsyncComponent(() => import('./HostMetricChart.vue'))

const props = defineProps({
  hostId: {
    type: String,
    required: true
  }
})

const activeMetric = ref('cpu')
const metricOptions = [
  { value: 'cpu', label: 'CPU' },
  { value: 'memory', label: '内存' },
  { value: 'tcp', label: 'TCP' }
]
const rangeOptions = [
  { value: '10m', label: '10 分钟' },
  { value: '30m', label: '30 分钟' },
  { value: '1h', label: '1 小时' }
]

const refreshLoop = createRefreshLoop({
  intervalMs: 5000,
  refresh: () => loadDetail({ silent: true })
})

onMounted(() => {
  void loadDetail()
  refreshLoop.start()
})

onUnmounted(() => {
  refreshLoop.stop()
})

watch(
  () => props.hostId,
  () => {
    void loadDetail()
  }
)

async function loadDetail(options = {}) {
  await store.loadHosts({ preferredHostId: props.hostId, ...options })
}

async function selectHistoryRange(range) {
  await store.setMetricHistoryRange(range)
}

function formatMemory(host) {
  return host ? `${host.memoryTotalGb} GB` : '--'
}

function formatPercent(value) {
  return typeof value === 'number' ? `${value.toFixed(1)}%` : '--'
}
</script>

<template>
  <main class="app-shell">
    <aside class="app-sidebar">
      <div class="app-brand">
        <ShieldCheck :size="25" aria-hidden="true" />
        <div>
          <strong>AegisMonitor</strong>
          <span>一体化监控平台</span>
        </div>
      </div>

      <nav class="app-nav" aria-label="Main navigation">
        <RouterLink class="app-nav__item is-active" to="/hosts">
          <Monitor :size="18" aria-hidden="true" />
          主机监控
        </RouterLink>
        <RouterLink class="app-nav__item" to="/services">
          <Signal :size="18" aria-hidden="true" />
          服务组件
        </RouterLink>
        <RouterLink class="app-nav__item" to="/alerts">
          <TriangleAlert :size="18" aria-hidden="true" />
          告警中心
        </RouterLink>
      </nav>
    </aside>

    <section class="app-main">
      <header class="app-header">
        <div>
          <p class="eyebrow">FE-0003</p>
          <h1>{{ store.selectedHost.value?.hostname || '主机详情' }}</h1>
        </div>
        <div class="toolbar">
          <RouterLink class="text-button" to="/hosts">
            <ArrowLeft :size="17" aria-hidden="true" />
            返回列表
          </RouterLink>
          <button class="icon-button" type="button" title="刷新主机详情" @click="loadDetail">
            <RefreshCw :class="{ spin: store.loading.value }" :size="18" aria-hidden="true" />
          </button>
        </div>
      </header>

      <div v-if="store.error.value" class="status-banner status-banner--warning">
        主机详情加载失败：{{ store.error.value }}
      </div>
      <div v-else-if="store.loading.value" class="status-banner">
        正在加载主机详情
      </div>

      <section v-if="store.hostNotFound.value" class="empty-state detail-empty">
        <Server :size="34" aria-hidden="true" />
        <strong>主机不存在或尚未接入</strong>
        <span>当前地址中的 `hostId` 为 {{ store.selectedHostId.value }}，请返回主机列表重新选择。</span>
      </section>

      <template v-else>
        <section class="detail-grid">
          <article class="detail-card detail-card--wide">
            <p class="eyebrow">Identity</p>
            <h2>{{ store.selectedHost.value?.alias || store.selectedHost.value?.id || '--' }}</h2>
            <div class="detail-facts">
              <span>IP：{{ store.selectedHost.value?.ipAddress || '--' }}</span>
              <span>
                类型：
                <b v-if="store.selectedHost.value?.kind === 'demo'" class="host-kind-text--demo">模拟主机</b>
                <b v-else-if="store.selectedHost.value?.kind === 'real'" class="host-kind-text--real">真实主机</b>
                <b v-else>--</b>
              </span>
              <span>系统：{{ store.selectedHost.value?.os || '--' }}</span>
              <span>规格：{{ store.selectedHost.value?.cpuCores ?? '--' }}C / {{ formatMemory(store.selectedHost.value) }}</span>
              <span>最近心跳：{{ store.selectedHost.value?.lastHeartbeatAt || '--' }}</span>
            </div>
          </article>

          <article class="detail-card">
            <Cpu :size="20" aria-hidden="true" />
            <span>CPU 使用率</span>
            <strong>{{ formatPercent(store.latestMetric.value?.cpuUsagePercent) }}</strong>
          </article>
          <article class="detail-card">
            <HardDrive :size="20" aria-hidden="true" />
            <span>内存使用率</span>
            <strong>{{ formatPercent(store.latestMetric.value?.memoryUsagePercent) }}</strong>
          </article>
          <article class="detail-card">
            <Network :size="20" aria-hidden="true" />
            <span>TCP 连接数</span>
            <strong>{{ store.latestMetric.value?.tcpConnectionCount ?? '--' }}</strong>
          </article>
        </section>

        <section class="host-panel metric-history-panel">
          <div class="panel-header metric-history-header">
            <div>
              <p class="eyebrow">Performance History</p>
              <h2>主机性能趋势</h2>
            </div>
            <span>{{ store.metricHistory.value.length }} 个数据点</span>
          </div>

          <div class="metric-history-toolbar">
            <div class="segmented-control" aria-label="趋势指标">
              <button
                v-for="option in metricOptions"
                :key="option.value"
                type="button"
                :class="{ 'is-active': activeMetric === option.value }"
                @click="activeMetric = option.value"
              >
                {{ option.label }}
              </button>
            </div>
            <div class="segmented-control" aria-label="时间范围">
              <button
                v-for="option in rangeOptions"
                :key="option.value"
                type="button"
                :disabled="store.historyLoading.value"
                :class="{ 'is-active': store.metricHistoryRange.value === option.value }"
                @click="selectHistoryRange(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <div v-if="store.historyError.value" class="status-banner status-banner--warning">
            历史指标加载失败：{{ store.historyError.value }}
          </div>
          <div v-if="store.historyLoading.value && store.metricHistory.value.length === 0" class="metric-history-empty">
            <RefreshCw class="spin" :size="20" aria-hidden="true" />
            <span>正在加载历史指标</span>
          </div>
          <div v-else-if="store.metricHistory.value.length === 0" class="metric-history-empty">
            <Activity :size="20" aria-hidden="true" />
            <span>等待 Agent 积累历史指标</span>
          </div>
          <HostMetricChart
            v-else
            :metric="activeMetric"
            :points="store.metricHistory.value"
          />

          <div class="metric-history-meta">
            <span>最新采样：{{ store.latestMetric.value?.reportedAt || '--' }}</span>
            <span>自动刷新：5 秒</span>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>
