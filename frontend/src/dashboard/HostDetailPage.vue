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
  { value: 'memory', label: '\u5185\u5b58' },
  { value: 'disk', label: '\u78c1\u76d8' },
  { value: 'networkRx', label: '\u7f51\u5361 RX' },
  { value: 'networkTx', label: '\u7f51\u5361 TX' },
  { value: 'tcp', label: 'TCP' }
]
const rangeOptions = [
  { value: '10m', label: '10 \u5206\u949f' },
  { value: '30m', label: '30 \u5206\u949f' },
  { value: '1h', label: '1 \u5c0f\u65f6' },
  { value: '6h', label: '6 \u5c0f\u65f6' },
  { value: '24h', label: '24 \u5c0f\u65f6' }
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

async function selectHistoryDate(event) {
  await store.setMetricHistoryDate(event.target.value)
}

function formatMemory(host) {
  return host ? `${host.memoryTotalGb} GB` : '--'
}

function formatPercent(value) {
  return typeof value === 'number' ? `${value.toFixed(1)}%` : '--'
}

function formatTraffic(metric) {
  if (!metric) {
    return '--'
  }
  return `\u2193 ${formatMb(metric.networkReceivedMb)} / \u2191 ${formatMb(metric.networkSentMb)}`
}

function formatMb(value) {
  return typeof value === 'number' ? `${value.toFixed(1)} MB` : '--'
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
          <article class="detail-card">
            <HardDrive :size="20" aria-hidden="true" />
            <span>&#x78C1;&#x76D8;&#x4F7F;&#x7528;&#x7387;</span>
            <strong>{{ formatPercent(store.latestMetric.value?.diskUsagePercent) }}</strong>
          </article>
          <article class="detail-card">
            <Network :size="20" aria-hidden="true" />
            <span>&#x7F51;&#x5361;&#x6536;&#x53D1;&#x6D41;&#x91CF;</span>
            <strong>{{ formatTraffic(store.latestMetric.value) }}</strong>
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
                :class="{ 'is-active': !store.metricHistoryDate.value && store.metricHistoryRange.value === option.value }"
                @click="selectHistoryRange(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <label class="history-date-picker">
              <span>&#x6309;&#x65E5;&#x671F;</span>
              <input
                type="date"
                :value="store.metricHistoryDate.value"
                :disabled="store.historyLoading.value"
                @change="selectHistoryDate"
              >
            </label>
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
            <span v-if="store.metricHistoryDate.value">&#x5F53;&#x524D;&#x65E5;&#x671F;&#xFF1A;{{ store.metricHistoryDate.value }}</span>
            <span v-else>&#x81EA;&#x52A8;&#x5237;&#x65B0;&#xFF1A;5 &#x79D2;</span>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>
