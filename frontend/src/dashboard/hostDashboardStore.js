import { computed, ref } from 'vue'

export function createHostDashboardStore(options) {
  const api = options.api
  const now = options.now ?? (() => new Date())
  const hosts = ref([])
  const selectedHostId = ref('')
  const latestMetric = ref(null)
  const services = ref([])
  const alerts = ref([])
  const demoSeedResult = ref(null)
  const loading = ref(false)
  const error = ref('')
  const lastUpdatedAt = ref('')

  const stats = computed(() => ({
    totalHosts: hosts.value.length,
    onlineHosts: hosts.value.filter((host) => host.status === 'ONLINE').length,
    demoHosts: hosts.value.filter((host) => host.kind === 'demo').length
  }))
  const selectedHost = computed(() =>
    hosts.value.find((host) => host.id === selectedHostId.value) ?? null
  )
  const hostNotFound = computed(() =>
    hosts.value.length > 0 && selectedHostId.value !== '' && selectedHost.value === null
  )
  const empty = computed(() => !loading.value && !error.value && hosts.value.length === 0)
  const servicesEmpty = computed(() => selectedHost.value !== null && services.value.length === 0)
  const alertsEmpty = computed(() => !loading.value && !error.value && alerts.value.length === 0)

  async function loadHosts(options = {}) {
    loading.value = true
    error.value = ''
    try {
      hosts.value = await api.listHosts()
      selectedHostId.value = options.preferredHostId ?? hosts.value[0]?.id ?? ''
      latestMetric.value = selectedHost.value
        ? await loadLatestMetric(selectedHostId.value)
        : null
      services.value = selectedHost.value
        ? await loadServices(selectedHostId.value)
        : []
      lastUpdatedAt.value = formatTime(now())
    } catch (caughtError) {
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    } finally {
      loading.value = false
    }
  }

  async function selectHost(hostId) {
    const previousHostId = selectedHostId.value
    const previousMetric = latestMetric.value
    const previousServices = services.value
    loading.value = true
    error.value = ''
    try {
      selectedHostId.value = hostId
      latestMetric.value = selectedHost.value
        ? await loadLatestMetric(hostId)
        : null
      services.value = selectedHost.value
        ? await loadServices(hostId)
        : []
    } catch (caughtError) {
      selectedHostId.value = previousHostId
      latestMetric.value = previousMetric
      services.value = previousServices
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    } finally {
      loading.value = false
    }
  }

  async function loadAlerts() {
    loading.value = true
    error.value = ''
    try {
      alerts.value = typeof api.listAlerts === 'function'
        ? await api.listAlerts()
        : []
      lastUpdatedAt.value = formatTime(now())
    } catch (caughtError) {
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    } finally {
      loading.value = false
    }
  }

  async function ackAlert(eventId, acknowledgement) {
    loading.value = true
    error.value = ''
    try {
      const acknowledgedAlert = await api.ackAlert(eventId, acknowledgement)
      alerts.value = alerts.value.map((alert) =>
        alert.eventId === eventId || alert.id === eventId
          ? acknowledgedAlert
          : alert
      )
      lastUpdatedAt.value = formatTime(now())
      return acknowledgedAlert
    } catch (caughtError) {
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
      return null
    } finally {
      loading.value = false
    }
  }

  async function seedDemoData() {
    loading.value = true
    error.value = ''
    try {
      const result = await api.seedDemoData()
      demoSeedResult.value = result
      await loadHosts()
      await loadAlerts()
      return result
    } catch (caughtError) {
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
      return null
    } finally {
      loading.value = false
    }
  }

  async function loadLatestMetric(hostId) {
    return hostId && typeof api.getLatestHostMetric === 'function'
      ? await api.getLatestHostMetric(hostId)
      : null
  }

  async function loadServices(hostId) {
    return hostId && typeof api.listServices === 'function'
      ? await api.listServices(hostId)
      : []
  }

  return {
    hosts,
    selectedHostId,
    selectedHost,
    hostNotFound,
    latestMetric,
    services,
    alerts,
    demoSeedResult,
    loading,
    error,
    empty,
    servicesEmpty,
    alertsEmpty,
    lastUpdatedAt,
    stats,
    loadHosts,
    selectHost,
    loadAlerts,
    ackAlert,
    seedDemoData
  }
}

function formatTime(date) {
  return date.toLocaleTimeString('zh-CN', {
    hour12: false
  })
}
