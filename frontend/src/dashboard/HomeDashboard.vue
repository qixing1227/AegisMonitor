<script setup>
import {
  Activity,
  Bell,
  CheckCircle2,
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
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { dashboardStore as store } from './dashboardStore.js'
import { createRefreshLoop } from './refreshLoop.js'

const showDemoSeed = import.meta.env.DEV || import.meta.env.VITE_AEGIS_DEMO === 'true'
const openAlertCount = computed(() => store.alerts.value.filter((alert) => alert.open).length)
const refreshLoop = createRefreshLoop({
  intervalMs: 5000,
  refresh: () => refreshDashboard({ silent: true })
})

onMounted(() => {
  void refreshDashboard()
  refreshLoop.start()
})

onUnmounted(() => {
  refreshLoop.stop()
})

function formatMemory(host) {
  return `${host.memoryTotalGb} GB`
}

function formatPercent(value) {
  return typeof value === 'number' ? `${value.toFixed(1)}%` : '--'
}

async function refreshDashboard(options = {}) {
  await store.loadHosts(options)
  await store.loadAlerts(options)
}

async function initializeDemoData() {
  await store.seedDemoData()
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
          <h1>主机监控台</h1>
        </div>
        <div class="toolbar">
          <span class="timestamp">更新 {{ store.lastUpdatedAt.value || '--' }}</span>
          <button
            v-if="showDemoSeed"
            class="text-button"
            type="button"
            @click="initializeDemoData"
          >
            <Bell :size="16" aria-hidden="true" />
            初始化演示数据
          </button>
          <button class="icon-button" type="button" title="刷新监控台" @click="refreshDashboard">
            <RefreshCw :class="{ spin: store.loading.value }" :size="18" aria-hidden="true" />
          </button>
        </div>
      </header>

      <section class="overview-grid" aria-label="Host summary">
        <article class="overview-card">
          <Server :size="21" aria-hidden="true" />
          <span>主机总数</span>
          <strong>{{ store.stats.value.totalHosts }}</strong>
        </article>
        <article class="overview-card">
          <CheckCircle2 :size="21" aria-hidden="true" />
          <span>在线主机</span>
          <strong>{{ store.stats.value.onlineHosts }}</strong>
        </article>
        <article class="overview-card">
          <Monitor :size="21" aria-hidden="true" />
          <span>模拟主机</span>
          <strong>{{ store.stats.value.demoHosts }}</strong>
        </article>
        <article class="overview-card">
          <TriangleAlert :size="21" aria-hidden="true" />
          <span>待处理告警</span>
          <strong>{{ openAlertCount }}</strong>
        </article>
      </section>

      <div v-if="store.demoSeedResult.value" class="status-banner">
        演示数据已准备：{{ store.demoSeedResult.value.hostsCreated }} 台主机、
        {{ store.demoSeedResult.value.servicesCreated }} 个服务、
        {{ store.demoSeedResult.value.alertsCreated }} 条告警。
      </div>

      <section class="metric-snapshot" aria-label="Latest host metric snapshot">
        <article class="metric-snapshot__hero">
          <div>
            <p class="eyebrow">Latest Snapshot</p>
            <h2>{{ store.selectedHost.value?.hostname || '未选择主机' }}</h2>
            <span>{{ store.latestMetric.value?.reportedAt || '等待 Agent 上报最新指标' }}</span>
          </div>
          <Activity :size="28" aria-hidden="true" />
        </article>
        <article class="metric-snapshot__card">
          <Cpu :size="20" aria-hidden="true" />
          <span>CPU 使用率</span>
          <strong>{{ formatPercent(store.latestMetric.value?.cpuUsagePercent) }}</strong>
        </article>
        <article class="metric-snapshot__card">
          <HardDrive :size="20" aria-hidden="true" />
          <span>内存使用率</span>
          <strong>{{ formatPercent(store.latestMetric.value?.memoryUsagePercent) }}</strong>
        </article>
        <article class="metric-snapshot__card">
          <Network :size="20" aria-hidden="true" />
          <span>TCP 连接数</span>
          <strong>{{ store.latestMetric.value?.tcpConnectionCount ?? '--' }}</strong>
        </article>
      </section>

      <div v-if="store.error.value" class="status-banner status-banner--warning">
        主机列表加载失败：{{ store.error.value }}
      </div>
      <div v-else-if="store.loading.value" class="status-banner">
        正在加载主机列表
      </div>

      <section class="host-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Agents</p>
            <h2>已接入主机</h2>
          </div>
          <span>{{ store.hosts.value.length }} records</span>
        </div>

        <div v-if="store.empty.value" class="empty-state">
          <Server :size="32" aria-hidden="true" />
          <strong>暂无主机接入</strong>
          <span>启动后端并运行 Agent，或调用演示数据 seed 接口后再刷新。</span>
        </div>

        <div v-else class="real-host-table">
          <div class="real-host-row real-host-row--head">
            <span>主机</span>
            <span>IP</span>
            <span>系统</span>
            <span>规格</span>
            <span>类型</span>
            <span>状态</span>
            <span>最近心跳</span>
          </div>
          <div v-for="host in store.hosts.value" :key="host.id" class="real-host-row">
            <strong>
              <RouterLink class="host-link" :to="`/hosts/${host.id}`">
                {{ host.hostname }}
              </RouterLink>
              <small>{{ host.alias || host.id }}</small>
            </strong>
            <span>{{ host.ipAddress }}</span>
            <span>{{ host.os || '--' }}</span>
            <span>{{ host.cpuCores }}C / {{ formatMemory(host) }}</span>
            <span class="host-kind" :class="host.kind === 'demo' ? 'kind-模拟' : 'kind-真实'">
              {{ host.kind === 'demo' ? '模拟' : '真实' }}
            </span>
            <span class="status-dot" :class="`status-${host.status}`">{{ host.status }}</span>
            <span class="heartbeat-cell">
              {{ host.lastHeartbeatAt || '--' }}
              <RouterLink class="detail-link" :to="`/hosts/${host.id}`">详情</RouterLink>
            </span>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>
