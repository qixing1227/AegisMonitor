import assert from 'node:assert/strict'
import test from 'node:test'
import { createAegisApi } from './aegisApi.js'

test('ops engineer can load host list without leaking agent secrets', async () => {
  const requests = []
  const api = createAegisApi({
    fetch: async (url) => {
      requests.push(url)
      return jsonResponse({
        success: true,
        code: 'OK',
        message: 'ok',
        data: [
          {
            agentId: 'demo_agt_001',
            hostId: 'demo_host_001',
            agentSecret: 'should-not-reach-the-page',
            hostname: 'demo-web-01',
            alias: '模拟 Web 主机',
            ipAddress: '10.0.0.11',
            osName: 'Windows Server',
            osVersion: '2022',
            cpuCores: 4,
            memoryTotalBytes: 8589934592,
            status: 'ONLINE',
            lastHeartbeatAt: '2026-06-04T17:30:00+08:00'
          }
        ]
      })
    }
  })

  const hosts = await api.listHosts()

  assert.deepEqual(requests, ['/api/agents'])
  assert.deepEqual(hosts, [
    {
      id: 'demo_host_001',
      agentId: 'demo_agt_001',
      hostname: 'demo-web-01',
      alias: '模拟 Web 主机',
      ipAddress: '10.0.0.11',
      os: 'Windows Server 2022',
      cpuCores: 4,
      memoryTotalBytes: 8589934592,
      memoryTotalGb: 8,
      status: 'ONLINE',
      lastHeartbeatAt: '2026-06-04T17:30:00+08:00',
      kind: 'demo'
    }
  ])
  assert.equal(Object.hasOwn(hosts[0], 'agentSecret'), false)
})

test('ops engineer can load latest metric snapshot for a selected host', async () => {
  const requests = []
  const api = createAegisApi({
    fetch: async (url) => {
      requests.push(url)
      return jsonResponse({
        success: true,
        code: 'OK',
        message: 'latest host metrics',
        data: {
          hostId: 'host_001',
          reportedAt: '2026-06-06T10:32:00+08:00',
          cpuUsagePercent: 42.6,
          memoryUsagePercent: 61.2,
          tcpConnectionCount: 128
        }
      })
    }
  })

  const metric = await api.getLatestHostMetric('host_001')

  assert.deepEqual(requests, ['/api/metrics/host/latest?hostId=host_001'])
  assert.deepEqual(metric, {
    hostId: 'host_001',
    reportedAt: '2026-06-06T10:32:00+08:00',
    cpuUsagePercent: 42.6,
    memoryUsagePercent: 61.2,
    tcpConnectionCount: 128
  })
})

test('ops engineer can load readable service list for a selected host', async () => {
  const requests = []
  const api = createAegisApi({
    fetch: async (url) => {
      requests.push(url)
      return jsonResponse({
        success: true,
        code: 'OK',
        message: 'service list',
        data: [
          {
            hostId: 'demo_host_001',
            serviceName: 'nginx',
            stackType: 'NGINX',
            processName: 'nginx.exe',
            pid: 8011,
            ports: [80, 443],
            status: 'RUNNING',
            commandLine: 'nginx -g daemon off;',
            lastSeenAt: '2026-06-06T10:40:00+08:00'
          }
        ]
      })
    }
  })

  const services = await api.listServices('demo_host_001')

  assert.deepEqual(requests, ['/api/services?hostId=demo_host_001'])
  assert.deepEqual(services, [
    {
      id: 'demo_host_001:NGINX:nginx',
      hostId: 'demo_host_001',
      serviceName: 'nginx',
      stackType: 'NGINX',
      processName: 'nginx.exe',
      pid: 8011,
      ports: [80, 443],
      portsText: '80, 443',
      status: 'RUNNING',
      commandLine: 'nginx -g daemon off;',
      lastSeenAt: '2026-06-06T10:40:00+08:00'
    }
  ])
})

test('ops engineer can load alert events with open state', async () => {
  const requests = []
  const api = createAegisApi({
    fetch: async (url) => {
      requests.push(url)
      return jsonResponse({
        success: true,
        code: 'OK',
        message: 'alert list',
        data: [
          {
            eventId: 'alert_001',
            ruleId: 'rule_cpu_high',
            hostId: 'demo_host_001',
            metricName: 'cpu_usage_percent',
            severity: 'CRITICAL',
            thresholdValue: 80,
            actualValue: 92.5,
            status: 'OPEN',
            occurredAt: '2026-06-06T10:45:00+08:00',
            acknowledgedBy: null,
            acknowledgedAt: null,
            ackNote: null
          }
        ]
      })
    }
  })

  const alerts = await api.listAlerts()

  assert.deepEqual(requests, ['/api/alerts'])
  assert.deepEqual(alerts, [
    {
      id: 'alert_001',
      eventId: 'alert_001',
      ruleId: 'rule_cpu_high',
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

test('ops engineer can acknowledge an alert with handling note', async () => {
  const requests = []
  const api = createAegisApi({
    fetch: async (url, options) => {
      requests.push({
        url,
        method: options?.method,
        headers: options?.headers,
        body: JSON.parse(options?.body)
      })
      return jsonResponse({
        success: true,
        code: 'OK',
        message: 'alert acknowledged',
        data: {
          eventId: 'alert_001',
          ruleId: 'rule_cpu_high',
          hostId: 'demo_host_001',
          metricName: 'cpu_usage_percent',
          severity: 'CRITICAL',
          thresholdValue: 80,
          actualValue: 92.5,
          status: 'ACKED',
          occurredAt: '2026-06-06T10:45:00+08:00',
          acknowledgedBy: 'ops01',
          acknowledgedAt: '2026-06-06T10:50:00+08:00',
          ackNote: '已扩容并持续观察'
        }
      })
    }
  })

  const alert = await api.ackAlert('alert_001', {
    acknowledgedBy: 'ops01',
    acknowledgedAt: '2026-06-06T10:50:00+08:00',
    ackNote: '已扩容并持续观察'
  })

  assert.deepEqual(requests, [
    {
      url: '/api/alerts/alert_001/ack',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: {
        acknowledgedBy: 'ops01',
        acknowledgedAt: '2026-06-06T10:50:00+08:00',
        ackNote: '已扩容并持续观察'
      }
    }
  ])
  assert.equal(alert.status, 'ACKED')
  assert.equal(alert.open, false)
  assert.equal(alert.acknowledgedBy, 'ops01')
  assert.equal(alert.ackNote, '已扩容并持续观察')
})

test('demo presenter can seed hosts services and alerts for the showcase', async () => {
  const requests = []
  const api = createAegisApi({
    fetch: async (url, options) => {
      requests.push({
        url,
        method: options?.method,
        headers: options?.headers,
        body: JSON.parse(options?.body)
      })
      return jsonResponse({
        success: true,
        code: 'OK',
        message: 'demo data seeded',
        data: {
          hostsCreated: 3,
          servicesCreated: 4,
          alertsCreated: 1
        }
      })
    }
  })

  const result = await api.seedDemoData()

  assert.deepEqual(requests, [
    {
      url: '/api/demo/seed',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: {
        hostCount: 3,
        includeServices: true,
        includeAlerts: true
      }
    }
  ])
  assert.deepEqual(result, {
    hostsCreated: 3,
    servicesCreated: 4,
    alertsCreated: 1
  })
})

function jsonResponse(body, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    async json() {
      return body
    }
  }
}
