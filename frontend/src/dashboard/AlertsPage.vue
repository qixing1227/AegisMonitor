<script setup>
import {
  Bell,
  CheckCircle2,
  Clock3,
  Monitor,
  RefreshCw,
  ShieldCheck,
  Signal,
  TriangleAlert,
  X
} from 'lucide-vue-next'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { dashboardStore as store } from './dashboardStore.js'
import { createRefreshLoop } from './refreshLoop.js'

const ackTarget = ref(null)
const ackError = ref('')
const ackForm = reactive({
  acknowledgedBy: 'ops01',
  acknowledgedAt: '',
  ackNote: ''
})

const openAlertCount = computed(() => store.alerts.value.filter((alert) => alert.open).length)
const acknowledgedAlertCount = computed(() =>
  store.alerts.value.filter((alert) => !alert.open).length
)

const refreshLoop = createRefreshLoop({
  intervalMs: 10000,
  refresh: () => store.loadAlerts()
})

onMounted(() => {
  void store.loadAlerts()
  refreshLoop.start()
})

onUnmounted(() => {
  refreshLoop.stop()
})

function startAck(alert) {
  ackTarget.value = alert
  ackError.value = ''
  ackForm.acknowledgedBy = ackForm.acknowledgedBy || 'ops01'
  ackForm.acknowledgedAt = toLocalDateTimeInput(new Date())
  ackForm.ackNote = ''
}

function cancelAck() {
  ackTarget.value = null
  ackError.value = ''
  ackForm.ackNote = ''
}

async function submitAck() {
  if (!ackTarget.value) {
    return
  }

  const acknowledgedBy = ackForm.acknowledgedBy.trim()
  const ackNote = ackForm.ackNote.trim()
  if (!acknowledgedBy || !ackNote) {
    ackError.value = '请填写确认人和处理备注'
    return
  }

  const acknowledgedAlert = await store.ackAlert(ackTarget.value.eventId, {
    acknowledgedBy,
    acknowledgedAt: toBackendDateTime(ackForm.acknowledgedAt),
    ackNote
  })
  if (acknowledgedAlert) {
    cancelAck()
    return
  }

  ackError.value = store.error.value || '告警确认失败'
}

function formatNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(1) : '--'
}

function toLocalDateTimeInput(date) {
  return [
    date.getFullYear(),
    '-',
    pad(date.getMonth() + 1),
    '-',
    pad(date.getDate()),
    'T',
    pad(date.getHours()),
    ':',
    pad(date.getMinutes())
  ].join('')
}

function toBackendDateTime(value) {
  if (!value) {
    return `${toLocalDateTimeInput(new Date())}:00${timezoneOffset(new Date())}`
  }

  const [datePart, timePart = '00:00'] = value.split('T')
  const normalizedTime = timePart.length === 5 ? `${timePart}:00` : timePart
  return `${datePart}T${normalizedTime}${timezoneOffset(new Date())}`
}

function timezoneOffset(date) {
  const offset = -date.getTimezoneOffset()
  const sign = offset >= 0 ? '+' : '-'
  const absolute = Math.abs(offset)
  return `${sign}${pad(Math.floor(absolute / 60))}:${pad(absolute % 60)}`
}

function pad(value) {
  return String(value).padStart(2, '0')
}
</script>

<template>
  <main class="app-shell">
    <aside class="app-sidebar">
      <div class="app-brand">
        <ShieldCheck :size="25" aria-hidden="true" />
        <div>
          <strong>AegisMonitor</strong>
          <span>一体化监控平台</span>
        </div>
      </div>

      <nav class="app-nav" aria-label="Main navigation">
        <RouterLink class="app-nav__item" to="/hosts">
          <Monitor :size="18" aria-hidden="true" />
          主机监控
        </RouterLink>
        <RouterLink class="app-nav__item" to="/services">
          <Signal :size="18" aria-hidden="true" />
          服务组件
        </RouterLink>
        <RouterLink class="app-nav__item is-active" to="/alerts">
          <TriangleAlert :size="18" aria-hidden="true" />
          告警中心
        </RouterLink>
      </nav>
    </aside>

    <section class="app-main">
      <header class="app-header">
        <div>
          <p class="eyebrow">FE-0005</p>
          <h1>告警中心</h1>
        </div>
        <div class="toolbar">
          <span class="timestamp">更新 {{ store.lastUpdatedAt.value || '--' }}</span>
          <button class="icon-button" type="button" title="刷新告警列表" @click="store.loadAlerts">
            <RefreshCw :class="{ spin: store.loading.value }" :size="18" aria-hidden="true" />
          </button>
        </div>
      </header>

      <section class="overview-grid" aria-label="Alert summary">
        <article class="overview-card">
          <TriangleAlert :size="21" aria-hidden="true" />
          <span>待处理告警</span>
          <strong>{{ openAlertCount }}</strong>
        </article>
        <article class="overview-card">
          <CheckCircle2 :size="21" aria-hidden="true" />
          <span>已确认告警</span>
          <strong>{{ acknowledgedAlertCount }}</strong>
        </article>
        <article class="overview-card">
          <Bell :size="21" aria-hidden="true" />
          <span>告警事件总数</span>
          <strong>{{ store.alerts.value.length }}</strong>
        </article>
      </section>

      <div v-if="store.error.value" class="status-banner status-banner--warning">
        告警列表加载失败：{{ store.error.value }}
      </div>
      <div v-else-if="store.loading.value" class="status-banner">
        正在加载告警列表
      </div>

      <section class="host-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Alerts</p>
            <h2>告警事件</h2>
          </div>
          <span>{{ store.alerts.value.length }} records</span>
        </div>

        <div v-if="store.alertsEmpty.value" class="empty-state">
          <Bell :size="34" aria-hidden="true" />
          <strong>暂无告警事件</strong>
          <span>当指标超过阈值后，告警事件会出现在这里，并支持运维人员确认处理。</span>
        </div>

        <div v-else class="alert-table">
          <div class="alert-row alert-row--head">
            <span>事件</span>
            <span>主机</span>
            <span>指标</span>
            <span>当前值</span>
            <span>阈值</span>
            <span>级别</span>
            <span>状态</span>
            <span>操作</span>
          </div>
          <div v-for="alert in store.alerts.value" :key="alert.id" class="alert-row">
            <strong>
              {{ alert.eventId }}
              <small>{{ alert.occurredAt || '--' }}</small>
            </strong>
            <span>{{ alert.hostId }}</span>
            <span>{{ alert.metricName }}</span>
            <span>{{ formatNumber(alert.actualValue) }}</span>
            <span>{{ formatNumber(alert.thresholdValue) }}</span>
            <span class="severity-pill" :class="`severity-${alert.severity}`">
              {{ alert.severity }}
            </span>
            <span class="status-dot" :class="`status-${alert.status}`">
              {{ alert.status }}
            </span>
            <span class="alert-actions">
              <button
                v-if="alert.open"
                class="text-button text-button--compact"
                type="button"
                @click="startAck(alert)"
              >
                <CheckCircle2 :size="16" aria-hidden="true" />
                ACK
              </button>
              <span v-else class="ack-summary">
                <Clock3 :size="15" aria-hidden="true" />
                {{ alert.acknowledgedBy || '已确认' }}
              </span>
            </span>
          </div>
        </div>
      </section>

      <section v-if="ackTarget" class="ack-panel" aria-label="Acknowledge alert">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Acknowledge</p>
            <h2>{{ ackTarget.eventId }}</h2>
          </div>
          <button class="icon-button" type="button" title="关闭确认面板" @click="cancelAck">
            <X :size="18" aria-hidden="true" />
          </button>
        </div>

        <form class="ack-form" @submit.prevent="submitAck">
          <label>
            确认人
            <input v-model="ackForm.acknowledgedBy" type="text" autocomplete="off" />
          </label>
          <label>
            确认时间
            <input v-model="ackForm.acknowledgedAt" type="datetime-local" />
          </label>
          <label class="ack-form__note">
            处理备注
            <textarea
              v-model="ackForm.ackNote"
              rows="3"
              placeholder="例如：已扩容并持续观察"
            />
          </label>

          <div v-if="ackError" class="status-banner status-banner--warning">
            {{ ackError }}
          </div>

          <div class="ack-actions">
            <button class="text-button" type="button" @click="cancelAck">取消</button>
            <button class="text-button text-button--primary" type="submit" :disabled="store.loading.value">
              <CheckCircle2 :size="16" aria-hidden="true" />
              确认告警
            </button>
          </div>
        </form>
      </section>
    </section>
  </main>
</template>
