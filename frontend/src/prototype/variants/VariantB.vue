<script setup>
import {
  Activity,
  AlertTriangle,
  Bell,
  CheckCircle2,
  Cpu,
  Database,
  Network,
  Play,
  RefreshCw,
  Server,
  ShieldCheck
} from 'lucide-vue-next'
import { computed } from 'vue'
import { formatBytes, formatDateTime, formatPercent, hostKind } from '../formatters'

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

const selectedAlert = computed(() =>
  props.alerts.find((alert) => alert.hostId === props.selectedHostId)
)
const selectedPressure = computed(() => {
  if (props.latestMetric?.cpuUsagePercent) {
    return props.latestMetric.cpuUsagePercent
  }
  if (selectedAlert.value?.actualValue) {
    return selectedAlert.value.actualValue
  }
  return 0
})
</script>

<template>
  <div class="war-room">
    <header class="war-room__header">
      <div class="brand-line">
        <ShieldCheck :size="22" aria-hidden="true" />
        <strong>AegisMonitor</strong>
        <span>Prototype B</span>
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

    <section class="command-strip">
      <div>
        <p class="eyebrow">Operations War Room</p>
        <h1>主机健康态势</h1>
      </div>
      <div class="strip-metrics">
        <span><Server :size="18" aria-hidden="true" />{{ stats.totalHosts }} hosts</span>
        <span><CheckCircle2 :size="18" aria-hidden="true" />{{ stats.onlineHosts }} online</span>
        <span><Bell :size="18" aria-hidden="true" />{{ stats.openAlerts }} open</span>
        <span><Database :size="18" aria-hidden="true" />{{ services.length }} services</span>
      </div>
    </section>

    <div v-if="error" class="status-banner status-banner--warning">
      后端接口不可用，当前显示样例数据：{{ error }}
    </div>
    <div v-if="loading" class="status-banner">正在加载监控数据</div>

    <section class="war-grid">
      <div class="host-matrix">
        <button
          v-for="host in hosts"
          :key="host.hostId"
          class="matrix-tile"
          :class="{ 'is-selected': host.hostId === selectedHostId, 'is-demo': hostKind(host) === '模拟' }"
          type="button"
          @click="$emit('select-host', host.hostId)"
        >
          <span class="matrix-tile__top">
            <strong>{{ host.hostname }}</strong>
            <small>{{ hostKind(host) }}</small>
          </span>
          <span class="matrix-tile__meta">{{ host.ipAddress }}</span>
          <span class="matrix-tile__status">
            <i :class="`status-${host.status}`"></i>{{ host.status }}
          </span>
        </button>
      </div>

      <aside class="selected-lane">
        <div class="lane-header">
          <Cpu :size="20" aria-hidden="true" />
          <div>
            <p class="eyebrow">Primary Host</p>
            <h2>{{ selectedHost?.hostname || '未选择主机' }}</h2>
          </div>
        </div>
        <div class="pressure-gauge">
          <span :style="{ height: `${Math.min(selectedPressure, 100)}%` }"></span>
        </div>
        <div class="lane-values">
          <div>
            <span>CPU</span>
            <strong>{{ formatPercent(latestMetric?.cpuUsagePercent || selectedAlert?.actualValue) }}</strong>
          </div>
          <div>
            <span>Memory</span>
            <strong>{{ formatPercent(latestMetric?.memoryUsagePercent) }}</strong>
          </div>
          <div>
            <span>TCP</span>
            <strong>{{ latestMetric?.tcpConnectionCount ?? '--' }}</strong>
          </div>
        </div>
      </aside>
    </section>

    <section class="war-bottom">
      <div class="live-table">
        <div class="panel-header">
          <h2>主机资源清单</h2>
          <span>{{ stats.demoHosts }} demo</span>
        </div>
        <div class="dense-table">
          <div class="dense-row dense-row--head">
            <span>Host</span>
            <span>OS</span>
            <span>Spec</span>
            <span>Heartbeat</span>
          </div>
          <div v-for="host in hosts" :key="host.hostId" class="dense-row">
            <strong>{{ host.hostname }}</strong>
            <span>{{ host.osName }} {{ host.osVersion }}</span>
            <span>{{ host.cpuCores }}C / {{ formatBytes(host.memoryTotalBytes) }}</span>
            <span>{{ formatDateTime(host.lastHeartbeatAt) }}</span>
          </div>
        </div>
      </div>

      <div class="alert-feed">
        <div class="panel-header">
          <h2>告警流</h2>
          <AlertTriangle :size="19" aria-hidden="true" />
        </div>
        <p v-if="alerts.length === 0" class="empty-text">暂无告警</p>
        <div v-for="alert in alerts" :key="alert.eventId" class="feed-item">
          <span :class="`severity-${alert.severity}`">{{ alert.severity }}</span>
          <strong>{{ alert.hostId }}</strong>
          <small>{{ alert.metricName }} / {{ alert.actualValue }}</small>
        </div>
      </div>

      <div class="signal-panel">
        <div class="panel-header">
          <h2>网络信号</h2>
          <Network :size="19" aria-hidden="true" />
        </div>
        <div class="signal-bars" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
        </div>
        <div class="metric-line">
          <Activity :size="18" aria-hidden="true" />
          <span>Selected TCP</span>
          <strong>{{ latestMetric?.tcpConnectionCount ?? '--' }}</strong>
        </div>
      </div>
    </section>
  </div>
</template>
