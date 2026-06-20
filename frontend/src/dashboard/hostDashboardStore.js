import { computed, ref } from 'vue'

export function createHostDashboardStore(options) {
  const api = options.api
  const now = options.now ?? (() => new Date())
  const hosts = ref([])
  const selectedHostId = ref('')
  const latestMetric = ref(null)
  const metricHistory = ref([])
  const metricHistoryRange = ref('10m')
  const historyLoading = ref(false)
  const historyError = ref('')
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
    const showLoading = options.silent !== true
    const previousHostId = selectedHostId.value
    const previousMetric = latestMetric.value
    const previousHistory = metricHistory.value
    const previousServices = services.value
    if (showLoading) {
      loading.value = true
    }
    error.value = ''
    try {
      hosts.value = await api.listHosts()
      selectedHostId.value = options.preferredHostId ?? selectedHostId.value ?? hosts.value[0]?.id ?? ''
      if (!selectedHostId.value && hosts.value.length > 0) {
        selectedHostId.value = hosts.value[0].id
      }

      await refreshSelectedHostData({
        previousHostId,
        previousMetric,
        previousHistory,
        previousServices
      })
      lastUpdatedAt.value = formatTime(now())
    } catch (caughtError) {
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    } finally {
      if (showLoading) {
        loading.value = false
      }
    }
  }

  async function selectHost(hostId, options = {}) {
    const previousHostId = selectedHostId.value
    const previousMetric = latestMetric.value
    const previousHistory = metricHistory.value
    const previousServices = services.value
    const showLoading = options.silent !== true
    if (showLoading) {
      loading.value = true
    }
    error.value = ''
    try {
      selectedHostId.value = hostId
      await refreshSelectedHostData({
        previousHostId,
        previousMetric,
        previousHistory,
        previousServices
      })
    } finally {
      if (showLoading) {
        loading.value = false
      }
    }
  }

  async function loadAlerts(options = {}) {
    const showLoading = options.silent !== true
    if (showLoading) {
      loading.value = true
    }
    error.value = ''
    try {
      alerts.value = typeof api.listAlerts === 'function'
        ? await api.listAlerts()
        : []
      lastUpdatedAt.value = formatTime(now())
    } catch (caughtError) {
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    } finally {
      if (showLoading) {
        loading.value = false
      }
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

  async function loadMetricHistory(hostId) {
    if (!hostId || typeof api.getHostMetricHistory !== 'function') {
      return []
    }
    const result = await api.getHostMetricHistory(hostId, metricHistoryRange.value)
    return result.points
  }

  async function setMetricHistoryRange(range) {
    if (!['10m', '30m', '1h'].includes(range)) {
      throw new Error(`Unsupported metric history range: ${range}`)
    }

    metricHistoryRange.value = range
    if (selectedHost.value === null) {
      metricHistory.value = []
      return
    }

    const previousHistory = metricHistory.value
    historyLoading.value = true
    historyError.value = ''
    try {
      metricHistory.value = await loadMetricHistory(selectedHostId.value)
    } catch (caughtError) {
      metricHistory.value = previousHistory
      historyError.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    } finally {
      historyLoading.value = false
    }
  }

  async function loadServices(hostId) {
    return hostId && typeof api.listServices === 'function'
      ? await api.listServices(hostId)
      : []
  }

  async function refreshSelectedHostData(previousState) {
    if (selectedHost.value === null) {
      latestMetric.value = null
      metricHistory.value = []
      historyError.value = ''
      services.value = []
      return
    }

    try {
      latestMetric.value = await loadLatestMetric(selectedHostId.value)
    } catch (caughtError) {
      latestMetric.value = previousState.previousHostId === selectedHostId.value
        ? previousState.previousMetric
        : null
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    }

    try {
      metricHistory.value = await loadMetricHistory(selectedHostId.value)
      historyError.value = ''
    } catch (caughtError) {
      metricHistory.value = previousState.previousHostId === selectedHostId.value
        ? previousState.previousHistory ?? []
        : []
      historyError.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    }

    try {
      services.value = await loadServices(selectedHostId.value)
    } catch (caughtError) {
      services.value = previousState.previousHostId === selectedHostId.value
        ? previousState.previousServices
        : []
      error.value = caughtError instanceof Error ? caughtError.message : String(caughtError)
    }
  }

  return {
    hosts,
    selectedHostId,
    selectedHost,
    hostNotFound,
    latestMetric,
    metricHistory,
    metricHistoryRange,
    historyLoading,
    historyError,
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
    seedDemoData,
    setMetricHistoryRange
  }
}

function formatTime(date) {
  return date.toLocaleTimeString('zh-CN', {
    hour12: false
  })
}
