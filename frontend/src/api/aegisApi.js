export function createAegisApi(options = {}) {
  const fetcher = options.fetch ?? globalThis.fetch
  if (!fetcher) {
    throw new Error('A fetch implementation is required')
  }

  return {
    async listHosts() {
      const data = await request(fetcher, '/api/agents')
      return Array.isArray(data) ? data.map(toHost) : []
    },
    async getLatestHostMetric(hostId) {
      const data = await request(
        fetcher,
        `/api/metrics/host/latest?hostId=${encodeURIComponent(hostId)}`
      )
      return toLatestMetric(data)
    },
    async listServices(hostId) {
      const data = await request(
        fetcher,
        `/api/services?hostId=${encodeURIComponent(hostId)}`
      )
      return Array.isArray(data) ? data.map(toService) : []
    },
    async listAlerts() {
      const data = await request(fetcher, '/api/alerts')
      return Array.isArray(data) ? data.map(toAlert) : []
    },
    async ackAlert(eventId, acknowledgement) {
      const data = await request(fetcher, `/api/alerts/${encodeURIComponent(eventId)}/ack`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          acknowledgedBy: acknowledgement.acknowledgedBy,
          acknowledgedAt: acknowledgement.acknowledgedAt,
          ackNote: acknowledgement.ackNote
        })
      })
      return toAlert(data)
    },
    async seedDemoData(options = {}) {
      const data = await request(fetcher, '/api/demo/seed', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          hostCount: options.hostCount ?? 3,
          includeServices: options.includeServices ?? true,
          includeAlerts: options.includeAlerts ?? true
        })
      })
      return toDemoSeedResult(data)
    }
  }
}

async function request(fetcher, path, options) {
  const response = await fetcher(path, options)
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`)
  }

  const body = await response.json()
  if (body?.success === false) {
    throw new Error(body.message ?? 'API request failed')
  }
  return body?.data ?? body
}

function toHost(agent) {
  const memoryTotalBytes = Number(agent.memoryTotalBytes ?? 0)
  return {
    id: agent.hostId,
    agentId: agent.agentId,
    hostname: agent.hostname,
    alias: agent.alias ?? '',
    ipAddress: agent.ipAddress,
    os: [agent.osName, agent.osVersion].filter(Boolean).join(' '),
    cpuCores: Number(agent.cpuCores ?? 0),
    memoryTotalBytes,
    memoryTotalGb: toGb(memoryTotalBytes),
    status: agent.status ?? 'UNKNOWN',
    lastHeartbeatAt: agent.lastHeartbeatAt ?? '',
    kind: isDemoHost(agent) ? 'demo' : 'real'
  }
}

function toLatestMetric(metric) {
  return {
    hostId: metric.hostId,
    reportedAt: metric.reportedAt ?? '',
    cpuUsagePercent: Number(metric.cpuUsagePercent ?? 0),
    memoryUsagePercent: Number(metric.memoryUsagePercent ?? 0),
    tcpConnectionCount: Number(metric.tcpConnectionCount ?? 0)
  }
}

function toService(service) {
  const ports = Array.isArray(service.ports) ? service.ports : []
  return {
    id: `${service.hostId}:${service.stackType}:${service.serviceName}`,
    hostId: service.hostId,
    serviceName: service.serviceName,
    stackType: service.stackType,
    processName: service.processName,
    pid: Number(service.pid ?? 0),
    ports,
    portsText: ports.length > 0 ? ports.join(', ') : '--',
    status: service.status ?? 'UNKNOWN',
    commandLine: service.commandLine ?? '',
    lastSeenAt: service.lastSeenAt ?? ''
  }
}

function toAlert(alert) {
  const status = alert.status ?? 'UNKNOWN'
  return {
    id: alert.eventId,
    eventId: alert.eventId,
    ruleId: alert.ruleId,
    hostId: alert.hostId,
    metricName: alert.metricName,
    severity: alert.severity ?? 'UNKNOWN',
    thresholdValue: Number(alert.thresholdValue ?? 0),
    actualValue: Number(alert.actualValue ?? 0),
    status,
    occurredAt: alert.occurredAt ?? '',
    acknowledgedBy: alert.acknowledgedBy ?? '',
    acknowledgedAt: alert.acknowledgedAt ?? '',
    ackNote: alert.ackNote ?? '',
    open: ['OPEN', 'ACTIVE', 'NEW'].includes(status)
  }
}

function toDemoSeedResult(result) {
  return {
    hostsCreated: Number(result.hostsCreated ?? 0),
    servicesCreated: Number(result.servicesCreated ?? 0),
    alertsCreated: Number(result.alertsCreated ?? 0)
  }
}

function toGb(bytes) {
  return Math.round((bytes / 1024 / 1024 / 1024) * 10) / 10
}

function isDemoHost(agent) {
  return agent.hostId?.startsWith('demo_') || agent.agentId?.startsWith('demo_')
}
