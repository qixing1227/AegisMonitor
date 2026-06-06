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
import { onMounted, onUnmounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { dashboardStore as store } from './dashboardStore.js'
import { createRefreshLoop } from './refreshLoop.js'

const props = defineProps({
  hostId: {
    type: String,
    required: true
  }
})

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

        <section class="host-panel">
          <div class="panel-header">
            <div>
              <p class="eyebrow">Runtime Snapshot</p>
              <h2>最新上报快照</h2>
            </div>
            <span>{{ store.latestMetric.value?.reportedAt || '等待 Agent 上报' }}</span>
          </div>
          <div class="snapshot-note">
            <Activity :size="20" aria-hidden="true" />
            <span>第一版只展示最新快照，历史曲线作为后续扩展。</span>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>
