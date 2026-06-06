import assert from 'node:assert/strict'
import test from 'node:test'
import { createHostDashboardStore } from './hostDashboardStore.js'

test('dashboard shows host totals after loading hosts', async () => {
  const store = createHostDashboardStore({
    api: {
      async listHosts() {
        return [
          {
            id: 'host_001',
            hostname: 'DESKTOP-AEGIS',
            alias: '答辩真实主机',
            ipAddress: '192.168.1.10',
            os: 'Windows 11',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'real'
          },
          {
            id: 'demo_host_001',
            hostname: 'demo-web-01',
            alias: '模拟 Web 主机',
            ipAddress: '10.0.0.11',
            os: 'Windows Server 2022',
            cpuCores: 4,
            memoryTotalGb: 8,
            status: 'ONLINE',
            kind: 'demo'
          }
        ]
      }
    },
    now: () => new Date('2026-06-06T10:30:00+08:00')
  })

  await store.loadHosts()

  assert.equal(store.loading.value, false)
  assert.equal(store.error.value, '')
  assert.equal(store.lastUpdatedAt.value, '10:30:00')
  assert.deepEqual(store.stats.value, {
    totalHosts: 2,
    onlineHosts: 2,
    demoHosts: 1
  })
  assert.equal(store.hosts.value[0].hostname, 'DESKTOP-AEGIS')
})

test('dashboard exposes an empty state when no hosts are connected', async () => {
  const store = createHostDashboardStore({
    api: {
      async listHosts() {
        return []
      }
    }
  })

  await store.loadHosts()

  assert.equal(store.empty.value, true)
  assert.deepEqual(store.stats.value, {
    totalHosts: 0,
    onlineHosts: 0,
    demoHosts: 0
  })
})

test('dashboard loads latest metric snapshot for the first host', async () => {
  const requests = []
  const store = createHostDashboardStore({
    api: {
      async listHosts() {
        return [
          {
            id: 'host_001',
            hostname: 'DESKTOP-AEGIS',
            alias: '答辩真实主机',
            ipAddress: '192.168.1.10',
            os: 'Windows 11',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'real'
          }
        ]
      },
      async getLatestHostMetric(hostId) {
        requests.push(hostId)
        return {
          hostId,
          reportedAt: '2026-06-06T10:32:00+08:00',
          cpuUsagePercent: 42.6,
          memoryUsagePercent: 61.2,
          tcpConnectionCount: 128
        }
      }
    }
  })

  await store.loadHosts()

  assert.deepEqual(requests, ['host_001'])
  assert.equal(store.selectedHostId.value, 'host_001')
  assert.equal(store.selectedHost.value.hostname, 'DESKTOP-AEGIS')
  assert.deepEqual(store.latestMetric.value, {
    hostId: 'host_001',
    reportedAt: '2026-06-06T10:32:00+08:00',
    cpuUsagePercent: 42.6,
    memoryUsagePercent: 61.2,
    tcpConnectionCount: 128
  })
})

test('ops engineer can select a host and load its detail metric snapshot', async () => {
  const metricRequests = []
  const store = createHostDashboardStore({
    api: {
      async listHosts() {
        return [
          {
            id: 'host_001',
            hostname: 'DESKTOP-AEGIS',
            alias: '答辩真实主机',
            ipAddress: '192.168.1.10',
            os: 'Windows 11',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'real'
          },
          {
            id: 'demo_host_002',
            hostname: 'demo-app-01',
            alias: '模拟应用主机',
            ipAddress: '10.0.0.12',
            os: 'Ubuntu 22.04',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'demo'
          }
        ]
      },
      async getLatestHostMetric(hostId) {
        metricRequests.push(hostId)
        return {
          hostId,
          reportedAt: '2026-06-06T10:35:00+08:00',
          cpuUsagePercent: hostId === 'demo_host_002' ? 71.4 : 42.6,
          memoryUsagePercent: hostId === 'demo_host_002' ? 58.8 : 61.2,
          tcpConnectionCount: hostId === 'demo_host_002' ? 96 : 128
        }
      }
    }
  })

  await store.loadHosts()
  await store.selectHost('demo_host_002')

  assert.deepEqual(metricRequests, ['host_001', 'demo_host_002'])
  assert.equal(store.selectedHostId.value, 'demo_host_002')
  assert.equal(store.selectedHost.value.hostname, 'demo-app-01')
  assert.deepEqual(store.latestMetric.value, {
    hostId: 'demo_host_002',
    reportedAt: '2026-06-06T10:35:00+08:00',
    cpuUsagePercent: 71.4,
    memoryUsagePercent: 58.8,
    tcpConnectionCount: 96
  })
})

test('direct host detail visit exposes not found when host is unknown', async () => {
  const metricRequests = []
  const store = createHostDashboardStore({
    api: {
      async listHosts() {
        return [
          {
            id: 'host_001',
            hostname: 'DESKTOP-AEGIS',
            alias: '答辩真实主机',
            ipAddress: '192.168.1.10',
            os: 'Windows 11',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'real'
          }
        ]
      },
      async getLatestHostMetric(hostId) {
        metricRequests.push(hostId)
        return {
          hostId,
          reportedAt: '2026-06-06T10:35:00+08:00',
          cpuUsagePercent: 42.6,
          memoryUsagePercent: 61.2,
          tcpConnectionCount: 128
        }
      }
    }
  })

  await store.loadHosts({ preferredHostId: 'missing_host' })

  assert.equal(store.selectedHostId.value, 'missing_host')
  assert.equal(store.selectedHost.value, null)
  assert.equal(store.hostNotFound.value, true)
  assert.equal(store.latestMetric.value, null)
  assert.deepEqual(metricRequests, [])
})

test('dashboard loads service list for the selected host', async () => {
  const serviceRequests = []
  const store = createHostDashboardStore({
    api: {
      async listHosts() {
        return [
          {
            id: 'demo_host_001',
            hostname: 'demo-web-01',
            alias: '模拟 Web 主机',
            ipAddress: '10.0.0.11',
            os: 'Windows Server 2022',
            cpuCores: 4,
            memoryTotalGb: 8,
            status: 'ONLINE',
            kind: 'demo'
          },
          {
            id: 'demo_host_002',
            hostname: 'demo-app-01',
            alias: '模拟应用主机',
            ipAddress: '10.0.0.12',
            os: 'Ubuntu 22.04',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'demo'
          }
        ]
      },
      async getLatestHostMetric(hostId) {
        return {
          hostId,
          reportedAt: '2026-06-06T10:42:00+08:00',
          cpuUsagePercent: 36.5,
          memoryUsagePercent: 55.2,
          tcpConnectionCount: 64
        }
      },
      async listServices(hostId) {
        serviceRequests.push(hostId)
        return hostId === 'demo_host_002'
          ? [
              {
                id: 'demo_host_002:SPRING_BOOT:aegis-business-api',
                hostId,
                serviceName: 'aegis-business-api',
                stackType: 'SPRING_BOOT',
                processName: 'java.exe',
                pid: 8012,
                ports: [8080],
                portsText: '8080',
                status: 'RUNNING',
                lastSeenAt: '2026-06-06T10:42:00+08:00'
              }
            ]
          : []
      }
    }
  })

  await store.loadHosts()
  await store.selectHost('demo_host_002')

  assert.deepEqual(serviceRequests, ['demo_host_001', 'demo_host_002'])
  assert.equal(store.servicesEmpty.value, false)
  assert.deepEqual(store.services.value, [
    {
      id: 'demo_host_002:SPRING_BOOT:aegis-business-api',
      hostId: 'demo_host_002',
      serviceName: 'aegis-business-api',
      stackType: 'SPRING_BOOT',
      processName: 'java.exe',
      pid: 8012,
      ports: [8080],
      portsText: '8080',
      status: 'RUNNING',
      lastSeenAt: '2026-06-06T10:42:00+08:00'
    }
  ])
})

test('dashboard loads alert events for the alert center', async () => {
  const store = createHostDashboardStore({
    api: {
      async listAlerts() {
        return [
          {
            id: 'alert_001',
            eventId: 'alert_001',
            hostId: 'demo_host_001',
            metricName: 'cpu_usage_percent',
            severity: 'CRITICAL',
            thresholdValue: 80,
            actualValue: 92.5,
            status: 'OPEN',
            occurredAt: '2026-06-06T10:45:00+08:00',
            acknowledgedBy: '',
            acknowledgedAt: '',
            ackNote: '',
            open: true
          }
        ]
      }
    },
    now: () => new Date('2026-06-06T10:55:00+08:00')
  })

  await store.loadAlerts()

  assert.equal(store.alertsEmpty.value, false)
  assert.equal(store.lastUpdatedAt.value, '10:55:00')
  assert.deepEqual(store.alerts.value, [
    {
      id: 'alert_001',
      eventId: 'alert_001',
      hostId: 'demo_host_001',
      metricName: 'cpu_usage_percent',
      severity: 'CRITICAL',
      thresholdValue: 80,
      actualValue: 92.5,
      status: 'OPEN',
      occurredAt: '2026-06-06T10:45:00+08:00',
      acknowledgedBy: '',
      acknowledgedAt: '',
      ackNote: '',
      open: true
    }
  ])
})

test('dashboard updates an alert after acknowledgement', async () => {
  const ackRequests = []
  const store = createHostDashboardStore({
    api: {
      async listAlerts() {
        return [
          {
            id: 'alert_001',
            eventId: 'alert_001',
            hostId: 'demo_host_001',
            metricName: 'cpu_usage_percent',
            severity: 'CRITICAL',
            thresholdValue: 80,
            actualValue: 92.5,
            status: 'OPEN',
            occurredAt: '2026-06-06T10:45:00+08:00',
            acknowledgedBy: '',
            acknowledgedAt: '',
            ackNote: '',
            open: true
          }
        ]
      },
      async ackAlert(eventId, acknowledgement) {
        ackRequests.push({ eventId, acknowledgement })
        return {
          id: eventId,
          eventId,
          hostId: 'demo_host_001',
          metricName: 'cpu_usage_percent',
          severity: 'CRITICAL',
          thresholdValue: 80,
          actualValue: 92.5,
          status: 'ACKED',
          occurredAt: '2026-06-06T10:45:00+08:00',
          acknowledgedBy: acknowledgement.acknowledgedBy,
          acknowledgedAt: acknowledgement.acknowledgedAt,
          ackNote: acknowledgement.ackNote,
          open: false
        }
      }
    }
  })

  await store.loadAlerts()
  await store.ackAlert('alert_001', {
    acknowledgedBy: 'ops01',
    acknowledgedAt: '2026-06-06T10:58:00+08:00',
    ackNote: '已确认并扩容'
  })

  assert.deepEqual(ackRequests, [
    {
      eventId: 'alert_001',
      acknowledgement: {
        acknowledgedBy: 'ops01',
        acknowledgedAt: '2026-06-06T10:58:00+08:00',
        ackNote: '已确认并扩容'
      }
    }
  ])
  assert.equal(store.alerts.value[0].status, 'ACKED')
  assert.equal(store.alerts.value[0].open, false)
  assert.equal(store.alerts.value[0].acknowledgedBy, 'ops01')
})

test('dashboard keeps the requested host selected when its metric snapshot fails', async () => {
  const store = createHostDashboardStore({
    api: {
      async listHosts() {
        return [
          {
            id: 'host_001',
            hostname: 'DESKTOP-AEGIS',
            alias: '答辩真实主机',
            ipAddress: '192.168.1.10',
            os: 'Windows 11',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'real'
          },
          {
            id: 'host_002',
            hostname: 'DESKTOP-OPS',
            alias: '运维演示主机',
            ipAddress: '192.168.1.11',
            os: 'Windows 11',
            cpuCores: 8,
            memoryTotalGb: 16,
            status: 'ONLINE',
            kind: 'real'
          }
        ]
      },
      async getLatestHostMetric(hostId) {
        if (hostId === 'host_002') {
          throw new Error('metric backend down')
        }

        return {
          hostId,
          reportedAt: '2026-06-06T11:05:00+08:00',
          cpuUsagePercent: 37.2,
          memoryUsagePercent: 54.4,
          tcpConnectionCount: 72
        }
      },
      async listServices() {
        return []
      }
    }
  })

  await store.loadHosts()
  await store.selectHost('host_002')

  assert.equal(store.error.value, 'metric backend down')
  assert.equal(store.selectedHostId.value, 'host_002')
  assert.equal(store.selectedHost.value.hostname, 'DESKTOP-OPS')
  assert.equal(store.latestMetric.value, null)
})

test('dashboard can initialize demo data and refresh showcase panels', async () => {
  const calls = []
  const store = createHostDashboardStore({
    api: {
      async seedDemoData() {
        calls.push('seedDemoData')
        return {
          hostsCreated: 3,
          servicesCreated: 4,
          alertsCreated: 1
        }
      },
      async listHosts() {
        calls.push('listHosts')
        return [
          {
            id: 'demo_host_001',
            hostname: 'demo-web-01',
            alias: '模拟 Web 主机',
            ipAddress: '10.0.0.11',
            os: 'Windows Server 2022',
            cpuCores: 4,
            memoryTotalGb: 8,
            status: 'ONLINE',
            kind: 'demo'
          }
        ]
      },
      async getLatestHostMetric(hostId) {
        calls.push(`getLatestHostMetric:${hostId}`)
        return {
          hostId,
          reportedAt: '2026-06-06T11:15:00+08:00',
          cpuUsagePercent: 42.1,
          memoryUsagePercent: 56.8,
          tcpConnectionCount: 88
        }
      },
      async listServices(hostId) {
        calls.push(`listServices:${hostId}`)
        return []
      },
      async listAlerts() {
        calls.push('listAlerts')
        return [
          {
            id: 'alert_demo_001',
            eventId: 'alert_demo_001',
            hostId: 'demo_host_003',
            metricName: 'CPU_HIGH',
            severity: 'CRITICAL',
            thresholdValue: 80,
            actualValue: 93.5,
            status: 'OPEN',
            occurredAt: '2026-06-06T11:15:00+08:00',
            acknowledgedBy: '',
            acknowledgedAt: '',
            ackNote: '',
            open: true
          }
        ]
      }
    }
  })

  const result = await store.seedDemoData()

  assert.deepEqual(calls, [
    'seedDemoData',
    'listHosts',
    'getLatestHostMetric:demo_host_001',
    'listServices:demo_host_001',
    'listAlerts'
  ])
  assert.deepEqual(result, {
    hostsCreated: 3,
    servicesCreated: 4,
    alertsCreated: 1
  })
  assert.deepEqual(store.demoSeedResult.value, result)
  assert.equal(store.hosts.value.length, 1)
  assert.equal(store.alerts.value.length, 1)
  assert.equal(store.error.value, '')
})
