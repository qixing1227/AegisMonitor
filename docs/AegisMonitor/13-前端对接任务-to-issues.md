# AegisMonitor 前端对接任务拆解

## 1. 拆解依据

本文件使用 `to-issues` 思路，把“进入 Vue 前端开发前需要做什么”拆成可独立交付的垂直切片。当前以后端真实已实现接口为准，而不是早期接口原型。

当前可直接对接的接口：

| 能力 | 接口 | 当前响应形态 |
| --- | --- | --- |
| Agent / 主机列表 | `GET /api/agents` | `data` 为数组 |
| 主机最新指标 | `GET /api/metrics/host/latest?hostId=...` | `data` 为单个指标快照 |
| 服务清单 | `GET /api/services?hostId=...` | `data` 为数组 |
| 告警列表 | `GET /api/alerts` | `data` 为数组 |
| 告警 ACK | `POST /api/alerts/{eventId}/ack` | `data` 为 ACK 后事件 |

当前不应直接依赖的早期接口：

| 早期接口 | 当前状态 | 前端策略 |
| --- | --- | --- |
| `GET /api/hosts` | 未实现 | 暂用 `GET /api/agents` 作为主机列表 |
| `GET /api/hosts/{hostId}/metrics?range=10m` | 未实现 | 第一版只展示最新快照 |
| `GET /api/alerts/events` | 未实现 | 暂用 `GET /api/alerts` |
| `GET /api/auth/login` | 未实现 | 第一版先做无登录演示台，登录作为后续切片 |
| `GET /api/topology/services` | 未实现 | 拓扑暂不进入第一版前端 |
| `POST /api/demo/seed` | 已实现 | 可创建模拟主机、服务实例和演示告警 |

## 2. 前端数据适配约定

为了降低返工，前端第一版统一做一个 API adapter，把真实接口响应映射成页面模型：

- 页面内部叫 `Host`，来源是后端 `AgentDashboardHttpResponse`。
- 页面内部叫 `LatestMetric`，来源是 `LatestHostMetricHttpResponse`。
- 页面内部叫 `ServiceInstance`，来源是 `ServiceDashboardHttpResponse`。
- 页面内部叫 `AlertEvent`，来源是后端 `AlertEvent`。

所有请求都先处理通用响应：

```ts
type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
}
```

## 3. Proposed Issues

### FE-0001 可启动的 Vue 监控台壳 + 主机列表 tracer bullet

- Type：DONE
- Blocked by：None - can start immediately
- User stories covered：运维工程师进入监控台后能看到已接入主机

What to build：

搭建 `frontend/` Vue 3 + Vite 应用，提供企业后台式基础布局，并完成第一条真实接口链路：页面启动后通过 `/api/agents` 拉取主机列表，显示主机数量、在线数量和主机表格。开发环境通过 Vite proxy 转发 `/api` 到 `http://localhost:8080`。

Acceptance criteria：

- [x] `frontend` 可以用 `npm install` 和 `npm run dev` 启动。
- [x] 首页不是落地页，而是直接进入监控台工作界面。
- [x] 页面通过真实 `GET /api/agents` 获取数据。
- [x] 表格展示 `hostname`、`alias`、`ipAddress`、`osName`、`cpuCores`、`memoryTotalBytes`、`status`、`lastHeartbeatAt`。
- [x] 有加载中、空数据、请求失败状态。

### FE-0002 总览仪表盘接入最新主机指标

- Type：DONE
- Blocked by：FE-0001
- User stories covered：运维工程师能在第一屏看到主机资源健康状态

What to build：

在总览仪表盘上选择一台默认主机，调用 `/api/metrics/host/latest?hostId=...`，展示 CPU 使用率、内存使用率、TCP 连接数和最近上报时间。第一版不做历史曲线，把趋势图位置做成“最新快照 + 后续曲线扩展”的稳定布局。

Acceptance criteria：

- [x] 有主机时自动选中第一台主机。
- [x] 调用真实 `GET /api/metrics/host/latest?hostId=...`。
- [x] CPU、内存、TCP 连接数以卡片或紧凑仪表方式展示。
- [x] 指标为空或接口返回错误时，页面显示可理解的空状态。
- [x] 手动刷新会同时刷新主机列表和当前主机最新指标。

### FE-0003 主机列表与主机详情的最小闭环

- Type：DONE
- Blocked by：FE-0001、FE-0002
- User stories covered：运维工程师能从主机列表进入单台主机查看详情

What to build：

提供 `/hosts` 和 `/hosts/:hostId` 两个前端路由。列表仍使用 `/api/agents`，详情页复用 Agent 信息和 `/api/metrics/host/latest`。这个切片不要求后端新增 `/api/hosts/{hostId}`，前端 adapter 负责从已加载的 Agent 列表中找到详情基础信息。

Acceptance criteria：

- [x] 点击主机表格行或详情按钮进入 `/hosts/:hostId`。
- [x] 详情页展示主机基础信息和最新指标快照。
- [x] 直接访问详情路由时，前端能重新拉取 `/api/agents` 并找到对应主机。
- [x] 找不到 `hostId` 时展示“主机不存在或尚未接入”。

### FE-0004 服务组件列表接入真实服务发现结果

- Type：DONE
- Blocked by：FE-0001
- User stories covered：运维工程师能看到 Agent 自动识别出的服务组件

What to build：

提供服务组件视图，先选择主机，再调用 `/api/services?hostId=...` 展示该主机的服务清单。支持按技术栈和运行状态在前端本地筛选。

Acceptance criteria：

- [x] 服务页可从主机列表选择主机。
- [x] 调用真实 `GET /api/services?hostId=...`。
- [x] 表格展示 `serviceName`、`stackType`、`processName`、`pid`、`ports`、`status`、`lastSeenAt`。
- [x] 端口列表可读，不出现数组直接挤爆单元格。
- [x] 没有服务时展示“等待 Agent 服务发现上报”。

### FE-0005 告警中心列表 + ACK 闭环

- Type：DONE
- Blocked by：FE-0001
- User stories covered：运维工程师能查看告警并确认处理

What to build：

提供告警中心视图，调用 `/api/alerts` 展示告警事件，并支持对未确认告警执行 ACK。ACK 表单收集处理人、处理时间和备注，提交到 `/api/alerts/{eventId}/ack`。

Acceptance criteria：

- [x] 告警列表调用真实 `GET /api/alerts`。
- [x] 表格展示 `eventId`、`hostId`、`metricName`、`severity`、`actualValue`、`thresholdValue`、`status`、`occurredAt`。
- [x] `OPEN`、`ACTIVE` 或未 ACK 的告警显示 ACK 操作。
- [x] ACK 成功后更新列表，状态变为 `ACKED`。
- [x] ACK 面板有备注输入和基础校验。

### FE-0006 前端自动刷新与接口错误治理

- Type：DONE
- Blocked by：FE-0002、FE-0004、FE-0005
- User stories covered：运维工程师能长时间盯大屏，不需要手动频繁刷新

What to build：

为总览、主机详情、服务组件和告警中心加入统一刷新机制。页面卸载时清理定时器；请求失败时保留旧数据并提示错误，避免整个界面闪空。

Acceptance criteria：

- [x] 总览和主机详情默认 5 秒刷新。
- [x] 服务组件和告警中心默认 10 秒刷新。
- [x] 页面卸载后没有残留定时器。
- [x] 请求失败时显示错误提示，但已有数据不被立即清空。
- [x] 用户可以手动刷新当前页面。

### DEMO-0001 演示数据入口：补齐 5 台主机展示

- Type：DONE
- Blocked by：None
- User stories covered：答辩者能展示 2 台真实主机 + 3 台模拟主机的课程设计规模

What to build：

已确认并实现后端 `POST /api/demo/seed` 演示数据入口。当前接口可创建 3 台模拟主机、若干服务实例和 1 条 CPU 高危告警，让前端能稳定展示课程设计规模。

Acceptance criteria：

- [x] 用户确认演示数据策略：采用后端 seed 接口。
- [x] 重复执行不会产生无限重复数据。
- [x] 后端模拟主机使用固定 `demo_host_*`、`demo_agt_*` 标识，前端可据此区分真实主机和模拟主机。
- [x] 演示步骤文档说明如何初始化演示数据。

### FE-0007 接入演示数据与答辩展示流

- Type：DONE
- Blocked by：FE-0003、FE-0004、FE-0005
- User stories covered：答辩者能按固定顺序展示主机、指标、服务、告警闭环

What to build：

把前端页面串成答辩展示流：总览 -> 主机列表 -> 主机详情 -> 服务组件 -> 告警中心 ACK。前端可提供一个仅开发环境显示的“初始化演示数据”按钮，调用已实现的 `POST /api/demo/seed`。

Acceptance criteria：

- [x] 入口页能看出系统当前主机数、在线数、告警数。
- [x] 可以从总览一路跳转到主机详情和服务组件。
- [x] 告警 ACK 能作为现场闭环演示。
- [x] 演示数据按钮只在开发/演示模式出现。
- [x] 文档记录完整答辩点击路径。

### FE-0008 第一版暂缓项归档

- Type：DONE
- Blocked by：FE-0007
- User stories covered：项目经理能控制范围，避免前端阶段膨胀

What to build：

明确第一版前端不做登录、用户管理、告警规则编辑、历史曲线、拓扑、真实权限控制。这些功能保留为文档设计或后续 sprint，避免影响主链路展示。

Acceptance criteria：

- [x] 用户确认第一版暂缓范围。
- [x] 页面导航不出现无法工作的入口。
- [x] 设计文档中保留这些能力的扩展说明。
- [x] 答辩话术说明“课程设计 MVP 已实现主链路，权限和拓扑为扩展设计”。

归档文档：

- `docs/AegisMonitor/14-MVP边界与暂缓项归档.md`

## 4. 推荐实施顺序

1. FE-0001：先让前端跑起来并读到真实主机。
2. FE-0002：把第一屏指标做出来。
3. FE-0003：完成主机列表到详情闭环。
4. FE-0004：接入服务发现结果。
5. FE-0005：接入告警列表和 ACK。
6. FE-0006：补刷新和错误状态。
7. FE-0007：串答辩展示流。
8. FE-0008：确认第一版暂缓项。

当前状态：FE-0001 到 FE-0008 已全部完成，第一版前端主链路进入提交前整理阶段。

## 5. 已确认决策

1. FE-0001 不再拆分，第一步直接做可启动 Vue 壳 + 主机列表 tracer bullet。
2. DEMO-0001 采用后端 seed 接口。
3. 第一版接受“暂不做登录”，先直接进入监控台。
4. 第一版接受“只展示最新指标快照”，暂不做历史曲线。
