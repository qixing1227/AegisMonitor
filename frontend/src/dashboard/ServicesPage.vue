<script setup>
import {
  Boxes,
  Database,
  Monitor,
  RefreshCw,
  Server,
  ShieldCheck,
  Signal,
  TriangleAlert
} from 'lucide-vue-next'
import { onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { dashboardStore as store } from './dashboardStore.js'
import { createRefreshLoop } from './refreshLoop.js'

const refreshLoop = createRefreshLoop({
  intervalMs: 10000,
  refresh: refreshServices
})

onMounted(() => {
  if (store.hosts.value.length === 0) {
    void store.loadHosts()
  }
  refreshLoop.start()
})

onUnmounted(() => {
  refreshLoop.stop()
})

async function selectHost(hostId) {
  await store.selectHost(hostId)
}

async function refreshServices() {
  if (store.hosts.value.length === 0) {
    await store.loadHosts()
    return
  }

  const hostId = store.selectedHostId.value || store.hosts.value[0]?.id || ''
  await store.selectHost(hostId)
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
        <RouterLink class="app-nav__item is-active" to="/services">
          <Signal :size="18" aria-hidden="true" />
          服务组件
        </RouterLink>
        <RouterLink class="app-nav__item" to="/alerts">
          <TriangleAlert :size="18" aria-hidden="true" />
          告警中心
        </RouterLink>
      </nav>
    </aside>

    <section class="app-main">
      <header class="app-header">
        <div>
          <p class="eyebrow">FE-0004</p>
          <h1>服务组件</h1>
        </div>
        <div class="toolbar">
          <span class="timestamp">更新 {{ store.lastUpdatedAt.value || '--' }}</span>
          <button class="icon-button" type="button" title="刷新服务组件" @click="refreshServices">
            <RefreshCw :class="{ spin: store.loading.value }" :size="18" aria-hidden="true" />
          </button>
        </div>
      </header>

      <div v-if="store.error.value" class="status-banner status-banner--warning">
        服务组件加载失败：{{ store.error.value }}
      </div>
      <div v-else-if="store.loading.value" class="status-banner">
        正在加载服务组件
      </div>

      <section class="service-layout">
        <aside class="service-hosts">
          <div class="panel-header">
            <div>
              <p class="eyebrow">Hosts</p>
              <h2>选择主机</h2>
            </div>
            <span>{{ store.hosts.value.length }}</span>
          </div>

          <button
            v-for="host in store.hosts.value"
            :key="host.id"
            class="service-host-button"
            :class="{ 'is-selected': host.id === store.selectedHostId.value }"
            type="button"
            @click="selectHost(host.id)"
          >
            <Server :size="17" aria-hidden="true" />
            <span>
              <strong>{{ host.hostname }}</strong>
              <small>{{ host.ipAddress }}</small>
            </span>
          </button>
        </aside>

        <section class="host-panel">
          <div class="panel-header">
            <div>
              <p class="eyebrow">Services</p>
              <h2>{{ store.selectedHost.value?.hostname || '未选择主机' }}</h2>
            </div>
            <span>{{ store.services.value.length }} services</span>
          </div>

          <div v-if="store.servicesEmpty.value" class="empty-state service-empty">
            <Boxes :size="34" aria-hidden="true" />
            <strong>等待 Agent 服务发现上报</strong>
            <span>当前主机尚未上报服务组件，运行 Agent 后再刷新。</span>
          </div>

          <div v-else class="service-table">
            <div class="service-row service-row--head">
              <span>服务名</span>
              <span>技术栈</span>
              <span>进程</span>
              <span>PID</span>
              <span>端口</span>
              <span>状态</span>
              <span>最后发现</span>
            </div>
            <div v-for="service in store.services.value" :key="service.id" class="service-row">
              <strong>{{ service.serviceName }}</strong>
              <span class="stack-chip">
                <Database :size="14" aria-hidden="true" />
                {{ service.stackType }}
              </span>
              <span>{{ service.processName }}</span>
              <span>{{ service.pid }}</span>
              <span>{{ service.portsText }}</span>
              <span class="status-dot" :class="`status-${service.status}`">{{ service.status }}</span>
              <span>{{ service.lastSeenAt || '--' }}</span>
            </div>
          </div>
        </section>
      </section>
    </section>
  </main>
</template>
