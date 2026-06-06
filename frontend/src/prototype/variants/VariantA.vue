<script setup>
import {
  Activity,
  AlertTriangle,
  Bell,
  CheckCircle2,
  Database,
  HardDrive,
  LayoutDashboard,
  Play,
  RefreshCw,
  Server,
  ShieldCheck
} from 'lucide-vue-next'
import { formatBytes, formatDateTime, formatPercent, hostKind, portsText } from '../formatters'

defineProps({
  hosts: {
    type: Array,
    required: true
  },
  alerts: {
    type: Array,
    required: true
  },
  services: {
    type: Array,
    required: true
  },
  selectedHost: {
    type: Object,
    default: null
  },
  selectedHostId: {
    type: String,
    required: true
  },
  latestMetric: {
    type: Object,
    default: null
  },
  stats: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  },
  refreshing: {
    type: Boolean,
    required: true
  },
  seeding: {
    type: Boolean,
    required: true
  },
  error: {
    type: String,
    required: true
  },
  dataSource: {
    type: String,
    required: true
  },
  lastUpdatedAt: {
    type: String,
    required: true
  }
})

defineEmits(['refresh', 'seed-demo', 'select-host'])
</script>

<template>
  <div class="ops-layout">
    <aside class="ops-sidebar">
      <div class="brand-block">
        <ShieldCheck :size="24" aria-hidden="true" />
        <div>
          <strong>AegisMonitor</strong>
          <span>Ops Engineer</span>
        </div>
      </div>
      <nav class="side-nav" aria-label="Main navigation">
        <a class="side-nav__item is-active" href="/prototype/dashboard?variant=A">
          <LayoutDashboard :size="17" aria-hidden="true" />
          总览仪表盘
        </a>
        <a class="side-nav__item" href="/prototype/dashboard?variant=A">
          <Server :size="17" aria-hidden="true" />
          主机监控
        </a>
        <a class="side-nav__item" href="/prototype/dashboard?variant=A">
          <Database :size="17" aria-hidden="true" />
          服务组件
        </a>
        <a class="side-nav__item" href="/prototype/dashboard?variant=A">
          <Bell :size="17" aria-hidden="true" />
          告警中心
        </a>
      </nav>
    </aside>

    <section class="ops-main">
      <header class="page-header">
        <div>
          <p class="eyebrow">Prototype A</p>
          <h1>一体化监控总览</h1>
        </div>
        <div class="toolbar">
          <span class="source-pill" :class="`source-${dataSource}`">{{ dataSource }}</span>
          <span class="timestamp">{{ lastUpdatedAt || '--' }}</span>
          <button class="icon-button" type="button" title="Seed demo data" @click="$emit('seed-demo')">
            <Play :size="18" aria-hidden="true" />
          </button>
          <button class="icon-button" type="button" title="Refresh" @click="$emit('refresh')">
            <RefreshCw :class="{ spin: refreshing || seeding }" :size="18" aria-hidden="true" />
          </button>
        </div>
      </header>

      <div v-if="error" class="status-banner status-banner--warning">
        后端接口不可用，当前显示样例数据：{{ error }}
      </div>
      <div v-if="loading" class="status-banner">正在加载监控数据</div>

      <section class="stat-grid">
        <article class="stat-card">
          <Server :size="20" aria-hidden="true" />
          <span>主机总数</span>
          <strong>{{ stats.totalHosts }}</strong>
        </article>
        <article class="stat-card">
          <CheckCircle2 :size="20" aria-hidden="true" />
          <span>在线 Agent</span>
          <strong>{{ stats.onlineHosts }}</strong>
        </article>
        <article class="stat-card">
          <AlertTriangle :size="20" aria-hidden="true" />
          <span>活跃告警</span>
          <strong>{{ stats.openAlerts }}</strong>
        </article>
        <article class="stat-card">
          <Activity :size="20" aria-hidden="true" />
          <span>当前 CPU</span>
          <strong>{{ formatPercent(latestMetric?.cpuUsagePercent) }}</strong>
        </article>
      </section>

      <section class="ops-grid">
        <div class="dashboard-panel panel-hosts">
          <div class="panel-header">
            <div>
              <p class="eyebrow">Hosts</p>
              <h2>主机接入列表</h2>
            </div>
            <span>{{ stats.demoHosts }} 台模拟</span>
          </div>
          <div class="host-table">
            <button
              v-for="host in hosts"
              :key="host.hostId"
              class="host-row"
              :class="{ 'is-selected': host.hostId === selectedHostId }"
              type="button"
              @click="$emit('select-host', host.hostId)"
            >
              <span>
                <strong>{{ host.hostname }}</strong>
                <small>{{ host.alias || host.hostId }}</small>
              </span>
              <span>{{ host.ipAddress }}</span>
              <span>{{ host.osName }} {{ host.osVersion }}</span>
              <span>{{ host.cpuCores }}C / {{ formatBytes(host.memoryTotalBytes) }}</span>
              <span class="host-kind" :class="`kind-${hostKind(host)}`">{{ hostKind(host) }}</span>
              <span class="status-dot" :class="`status-${host.status}`">{{ host.status }}</span>
            </button>
          </div>
        </div>

        <aside class="dashboard-panel panel-detail">
          <div class="panel-header">
            <div>
              <p class="eyebrow">Selected</p>
              <h2>{{ selectedHost?.hostname || '未选择主机' }}</h2>
            </div>
          </div>
          <div class="metric-stack">
            <div class="metric-line">
              <Activity :size="18" aria-hidden="true" />
              <span>CPU</span>
              <strong>{{ formatPercent(latestMetric?.cpuUsagePercent) }}</strong>
            </div>
            <div class="meter">
              <span :style="{ width: `${Math.min(latestMetric?.cpuUsagePercent || 0, 100)}%` }"></span>
            </div>
            <div class="metric-line">
              <HardDrive :size="18" aria-hidden="true" />
              <span>内存</span>
              <strong>{{ formatPercent(latestMetric?.memoryUsagePercent) }}</strong>
            </div>
            <div class="meter meter--memory">
              <span :style="{ width: `${Math.min(latestMetric?.memoryUsagePercent || 0, 100)}%` }"></span>
            </div>
          </div>

          <div class="mini-list">
            <h3>服务实例</h3>
            <p v-if="services.length === 0" class="empty-text">等待 Agent 服务发现上报</p>
            <div v-for="service in services" :key="`${service.hostId}-${service.serviceName}`" class="mini-row">
              <span>{{ service.serviceName }}</span>
              <strong>{{ service.stackType }}</strong>
              <small>{{ portsText(service.ports) }}</small>
            </div>
          </div>

          <div class="mini-list">
            <h3>最近告警</h3>
            <p v-if="alerts.length === 0" class="empty-text">暂无告警</p>
            <div v-for="alert in alerts.slice(0, 3)" :key="alert.eventId" class="alert-row">
              <span :class="`severity-${alert.severity}`">{{ alert.severity }}</span>
              <strong>{{ alert.hostId }}</strong>
              <small>{{ formatDateTime(alert.occurredAt) }}</small>
            </div>
          </div>
        </aside>
      </section>
    </section>
  </div>
</template>
