<script setup>
import {
  Activity,
  AlertTriangle,
  Bell,
  CheckCircle2,
  Database,
  HardDrive,
  Play,
  RefreshCw,
  Search,
  Server,
  ShieldCheck
} from 'lucide-vue-next'
import { computed, ref } from 'vue'
import { formatBytes, formatDateTime, formatPercent, hostKind, portsText } from '../formatters'

const props = defineProps({
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

const query = ref('')
const filteredHosts = computed(() => {
  const value = query.value.trim().toLowerCase()
  if (!value) {
    return props.hosts
  }
  return props.hosts.filter((host) =>
    [host.hostname, host.alias, host.ipAddress, host.osName]
      .filter(Boolean)
      .some((field) => field.toLowerCase().includes(value))
  )
})
const selectedAlerts = computed(() =>
  props.alerts.filter((alert) => alert.hostId === props.selectedHostId)
)
</script>

<template>
  <div class="workbench">
    <header class="workbench__topbar">
      <div class="brand-line">
        <ShieldCheck :size="22" aria-hidden="true" />
        <strong>AegisMonitor</strong>
        <span>Prototype C</span>
      </div>
      <div class="topbar-stats">
        <span><Server :size="17" aria-hidden="true" />{{ stats.totalHosts }}</span>
        <span><CheckCircle2 :size="17" aria-hidden="true" />{{ stats.onlineHosts }}</span>
        <span><Bell :size="17" aria-hidden="true" />{{ stats.openAlerts }}</span>
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

    <section class="workbench-grid">
      <aside class="host-rail">
        <div class="rail-search">
          <Search :size="17" aria-hidden="true" />
          <input v-model="query" type="search" placeholder="搜索主机" />
        </div>
        <div class="rail-list">
          <button
            v-for="host in filteredHosts"
            :key="host.hostId"
            class="rail-host"
            :class="{ 'is-selected': host.hostId === selectedHostId }"
            type="button"
            @click="$emit('select-host', host.hostId)"
          >
            <span>
              <strong>{{ host.hostname }}</strong>
              <small>{{ host.ipAddress }}</small>
            </span>
            <i :class="`status-${host.status}`"></i>
          </button>
        </div>
      </aside>

      <section class="host-workspace">
        <div class="workspace-title">
          <div>
            <p class="eyebrow">Host Workbench</p>
            <h1>{{ selectedHost?.hostname || '未选择主机' }}</h1>
          </div>
          <span v-if="selectedHost" class="host-kind" :class="`kind-${hostKind(selectedHost)}`">
            {{ hostKind(selectedHost) }}
          </span>
        </div>

        <div class="identity-grid">
          <div>
            <span>别名</span>
            <strong>{{ selectedHost?.alias || '--' }}</strong>
          </div>
          <div>
            <span>系统</span>
            <strong>{{ selectedHost?.osName || '--' }} {{ selectedHost?.osVersion || '' }}</strong>
          </div>
          <div>
            <span>规格</span>
            <strong>{{ selectedHost?.cpuCores ?? '--' }}C / {{ formatBytes(selectedHost?.memoryTotalBytes) }}</strong>
          </div>
          <div>
            <span>最近心跳</span>
            <strong>{{ formatDateTime(selectedHost?.lastHeartbeatAt) }}</strong>
          </div>
        </div>

        <div class="metric-dashboard">
          <article class="metric-card">
            <Activity :size="20" aria-hidden="true" />
            <span>CPU</span>
            <strong>{{ formatPercent(latestMetric?.cpuUsagePercent) }}</strong>
            <div class="meter">
              <span :style="{ width: `${Math.min(latestMetric?.cpuUsagePercent || 0, 100)}%` }"></span>
            </div>
          </article>
          <article class="metric-card">
            <HardDrive :size="20" aria-hidden="true" />
            <span>内存</span>
            <strong>{{ formatPercent(latestMetric?.memoryUsagePercent) }}</strong>
            <div class="meter meter--memory">
              <span :style="{ width: `${Math.min(latestMetric?.memoryUsagePercent || 0, 100)}%` }"></span>
            </div>
          </article>
          <article class="metric-card">
            <Database :size="20" aria-hidden="true" />
            <span>TCP</span>
            <strong>{{ latestMetric?.tcpConnectionCount ?? '--' }}</strong>
            <div class="tcp-dots" aria-hidden="true">
              <span></span><span></span><span></span><span></span>
            </div>
          </article>
        </div>

        <div class="service-strip">
          <div class="panel-header">
            <h2>该主机服务</h2>
            <span>{{ services.length }}</span>
          </div>
          <p v-if="services.length === 0" class="empty-text">等待 Agent 服务发现上报</p>
          <div v-for="service in services" :key="`${service.hostId}-${service.serviceName}`" class="service-chip">
            <strong>{{ service.serviceName }}</strong>
            <span>{{ service.stackType }}</span>
            <small>{{ portsText(service.ports) }}</small>
          </div>
        </div>
      </section>

      <aside class="action-rail">
        <div class="rail-panel">
          <div class="panel-header">
            <h2>告警处理</h2>
            <AlertTriangle :size="19" aria-hidden="true" />
          </div>
          <p v-if="selectedAlerts.length === 0" class="empty-text">当前主机暂无告警</p>
          <div v-for="alert in selectedAlerts" :key="alert.eventId" class="action-alert">
            <span :class="`severity-${alert.severity}`">{{ alert.severity }}</span>
            <strong>{{ alert.metricName }}</strong>
            <small>{{ alert.actualValue }} / {{ alert.thresholdValue }}</small>
          </div>
        </div>

        <div class="rail-panel">
          <div class="panel-header">
            <h2>全局告警</h2>
            <span>{{ alerts.length }}</span>
          </div>
          <div v-for="alert in alerts.slice(0, 4)" :key="alert.eventId" class="global-alert">
            <strong>{{ alert.hostId }}</strong>
            <small>{{ formatDateTime(alert.occurredAt) }}</small>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>
