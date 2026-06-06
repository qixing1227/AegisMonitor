export function formatBytes(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--'
  }
  const gb = value / 1024 / 1024 / 1024
  return `${gb.toFixed(gb >= 10 ? 0 : 1)} GB`
}

export function formatPercent(value) {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--'
  }
  return `${value.toFixed(1)}%`
}

export function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

export function portsText(ports) {
  return Array.isArray(ports) && ports.length > 0 ? ports.join(', ') : '--'
}

export function hostKind(host) {
  return host?.hostId?.startsWith('demo_') || host?.agentId?.startsWith('demo_') ? '模拟' : '真实'
}
