<!--
PROTOTYPE: Three variants of the AegisMonitor dashboard, switchable via
`?variant=`, on the throwaway `/prototype/dashboard` route.
-->
<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import VariantA from './variants/VariantA.vue'
import VariantB from './variants/VariantB.vue'
import VariantC from './variants/VariantC.vue'
import PrototypeSwitcher from './PrototypeSwitcher.vue'

const variants = ['A', 'B', 'C']
const labels = {
  A: 'Ops Console',
  B: 'War Room',
  C: 'Host Workbench'
}
const components = {
  A: VariantA,
  B: VariantB,
  C: VariantC
}

const hosts = ref([])
const alerts = ref([])
const services = ref([])
const latestMetric = ref(null)
const selectedHostId = ref('')
const loading = ref(false)
const refreshing = ref(false)
const seeding = ref(false)
const error = ref('')
const dataSource = ref('real')
const lastUpdatedAt = ref('')
const variant = ref(readVariant())

const currentComponent = computed(() => components[variant.value] ?? VariantA)
const selectedHost = computed(() =>
  hosts.value.find((host) => host.hostId === selectedHostId.value) ?? hosts.value[0] ?? null
)
const onlineHosts = computed(() =>
  hosts.value.filter((host) => host.status === 'ONLINE').length
)
const demoHosts = computed(() =>
  hosts.value.filter((host) => isDemoHost(host)).length
)
const openAlerts = computed(() =>
  alerts.value.filter((alert) => ['OPEN', 'ACTIVE', 'NEW'].includes(alert.status)).length
)
const criticalAlerts = computed(() =>
  alerts.value.filter((alert) => alert.severity === 'CRITICAL').length
)
const stateSummary = computed(() => ({
  source: dataSource.value,
  variant: variant.value,
  hosts: hosts.value.length,
  online: onlineHosts.value,
  alerts: alerts.value.length,
  selectedHostId: selectedHost.value?.hostId ?? 'none',
  services: services.value.length
}))
const dashboardStats = computed(() => ({
  totalHosts: hosts.value.length,
  onlineHosts: onlineHosts.value,
  demoHosts: demoHosts.value,
  openAlerts: openAlerts.value,
  criticalAlerts: criticalAlerts.value,
  serviceCount: services.value.length
}))

watch(selectedHostId, () => {
  void refreshSelectedHost()
})

onMounted(() => {
  ensurePrototypePath()
  window.addEventListener('popstate', syncVariantFromUrl)
  void refreshAll()
})

onUnmounted(() => {
  window.removeEventListener('popstate', syncVariantFromUrl)
})

function readVariant() {
  const queryVariant = new URLSearchParams(window.location.search).get('variant')
  return variants.includes(queryVariant) ? queryVariant : 'A'
}

function syncVariantFromUrl() {
  variant.value = readVariant()
}

function setVariant(nextVariant) {
  if (!variants.includes(nextVariant)) {
    return
  }
  variant.value = nextVariant
  const url = new URL(window.location.href)
  url.searchParams.set('variant', nextVariant)
  window.history.replaceState({}, '', url)
}

function ensurePrototypePath() {
  if (window.location.pathname === '/prototype/dashboard') {
    return
  }
  const url = new URL(window.location.href)
  url.pathname = '/prototype/dashboard'
  if (!url.searchParams.has('variant')) {
    url.searchParams.set('variant', variant.value)
  }
  window.history.replaceState({}, '', url)
}

async function refreshAll() {
  loading.value = hosts.value.length === 0
  refreshing.value = true
  error.value = ''

  try {
    const [hostData, alertData] = await Promise.all([
      apiGet('/api/agents'),
      apiGet('/api/alerts').catch(() => [])
    ])

    hosts.value = Array.isArray(hostData) ? hostData : []
    alerts.value = Array.isArray(alertData) ? alertData : []
    dataSource.value = 'real'

    if (!selectedHostId.value || !hosts.value.some((host) => host.hostId === selectedHostId.value)) {
      selectedHostId.value = hosts.value[0]?.hostId ?? ''
    } else {
      await refreshSelectedHost()
    }

    lastUpdatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (caughtError) {
    const sample = sampleState()
    hosts.value = sample.hosts
    alerts.value = sample.alerts
    services.value = sample.services
    latestMetric.value = sample.latestMetric
    selectedHostId.value = sample.hosts[0]?.hostId ?? ''
    dataSource.value = 'sample'
    error.value = readableError(caughtError)
    lastUpdatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function refreshSelectedHost() {
  if (!selectedHostId.value || dataSource.value === 'sample') {
    return
  }

  const hostId = encodeURIComponent(selectedHostId.value)
  const [metricData, serviceData] = await Promise.all([
    apiGet(`/api/metrics/host/latest?hostId=${hostId}`).catch(() => null),
    apiGet(`/api/services?hostId=${hostId}`).catch(() => [])
  ])
  latestMetric.value = metricData
  services.value = Array.isArray(serviceData) ? serviceData : []
}

async function seedDemoData() {
  seeding.value = true
  error.value = ''
  try {
    await apiPost('/api/demo/seed', {
      hostCount: 3,
      includeServices: true,
      includeAlerts: true
    })
    await refreshAll()
  } catch (caughtError) {
    error.value = readableError(caughtError)
  } finally {
    seeding.value = false
  }
}

async function apiGet(path) {
  const response = await fetch(path)
  return unwrapApiResponse(response)
}

async function apiPost(path, payload) {
  const response = await fetch(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })
  return unwrapApiResponse(response)
}

async function unwrapApiResponse(response) {
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`)
  }

  const body = await response.json()
  if (body?.success === false) {
    throw new Error(body.message ?? 'API request failed')
  }
  return body?.data ?? body
}

function selectHost(hostId) {
  selectedHostId.value = hostId
}

function isDemoHost(host) {
  return host.hostId?.startsWith('demo_') || host.agentId?.startsWith('demo_')
}

function readableError(caughtError) {
  return caughtError instanceof Error ? caughtError.message : String(caughtError)
}

function sampleState() {
  return {
    hosts: [
      {
        agentId: 'agt_001',
        hostId: 'host_001',
        hostname: 'DESKTOP-AEGIS',
        alias: '答辩真实主机',
        ipAddress: '192.168.1.10',
        osName: 'Windows',
        osVersion: '11',
        cpuCores: 8,
        memoryTotalBytes: 17179869184,
        status: 'ONLINE',
        lastHeartbeatAt: '2026-06-05T21:40:00+08:00'
      },
      {
        agentId: 'demo_agt_001',
        hostId: 'demo_host_001',
        hostname: 'demo-web-01',
        alias: '模拟 Web 主机',
        ipAddress: '10.0.0.11',
        osName: 'Windows Server',
        osVersion: '2022',
        cpuCores: 4,
        memoryTotalBytes: 8589934592,
        status: 'ONLINE',
        lastHeartbeatAt: '2026-06-04T17:30:00+08:00'
      },
      {
        agentId: 'demo_agt_002',
        hostId: 'demo_host_002',
        hostname: 'demo-app-01',
        alias: '模拟应用主机',
        ipAddress: '10.0.0.12',
        osName: 'Ubuntu',
        osVersion: '22.04',
        cpuCores: 8,
        memoryTotalBytes: 17179869184,
        status: 'ONLINE',
        lastHeartbeatAt: '2026-06-04T17:30:00+08:00'
      },
      {
        agentId: 'demo_agt_003',
        hostId: 'demo_host_003',
        hostname: 'demo-db-01',
        alias: '模拟数据库主机',
        ipAddress: '10.0.0.13',
        osName: 'CentOS',
        osVersion: '7.9',
        cpuCores: 8,
        memoryTotalBytes: 34359738368,
        status: 'ONLINE',
        lastHeartbeatAt: '2026-06-04T17:30:00+08:00'
      }
    ],
    alerts: [
      {
        eventId: 'demo_alert_cpu_high',
        hostId: 'demo_host_003',
        metricName: 'CPU_HIGH',
        severity: 'CRITICAL',
        thresholdValue: 80,
        actualValue: 93.5,
        status: 'OPEN',
        occurredAt: '2026-06-04T17:30:00+08:00'
      }
    ],
    services: [
      {
        hostId: 'host_001',
        serviceName: 'aegis-backend',
        stackType: 'SPRING_BOOT',
        processName: 'java.exe',
        pid: 10240,
        ports: [8080],
        status: 'RUNNING',
        lastSeenAt: '2026-06-05T21:40:00+08:00'
      },
      {
        hostId: 'host_001',
        serviceName: 'mysql',
        stackType: 'MYSQL',
        processName: 'mysqld.exe',
        pid: 3306,
        ports: [3306],
        status: 'RUNNING',
        lastSeenAt: '2026-06-05T21:40:00+08:00'
      }
    ],
    latestMetric: {
      hostId: 'host_001',
      reportedAt: '2026-06-05T21:40:00+08:00',
      cpuUsagePercent: 42.6,
      memoryUsagePercent: 61.2,
      tcpConnectionCount: 128
    }
  }
}
</script>

<template>
  <main class="prototype-page" :class="`variant-${variant.toLowerCase()}`">
    <component
      :is="currentComponent"
      :hosts="hosts"
      :alerts="alerts"
      :services="services"
      :selected-host="selectedHost"
      :selected-host-id="selectedHostId"
      :latest-metric="latestMetric"
      :stats="dashboardStats"
      :loading="loading"
      :refreshing="refreshing"
      :seeding="seeding"
      :error="error"
      :data-source="dataSource"
      :last-updated-at="lastUpdatedAt"
      @refresh="refreshAll"
      @seed-demo="seedDemoData"
      @select-host="selectHost"
    />

    <PrototypeSwitcher
      :variants="variants"
      :labels="labels"
      :current="variant"
      :state="stateSummary"
      @change="setVariant"
    />
  </main>
</template>
