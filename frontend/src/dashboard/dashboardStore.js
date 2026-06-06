import { createAegisApi } from '../api/aegisApi.js'
import { createHostDashboardStore } from './hostDashboardStore.js'

export const dashboardStore = createHostDashboardStore({
  api: createAegisApi()
})
