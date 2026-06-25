# AegisMonitor

AegisMonitor 是一个面向《软件工程》课程设计的**一体化监控平台 MVP**。项目围绕“真实主机接入、指标采集、服务发现、告警处理、答辩可展示”这条主线开发，目标是在课程设计规模内交付一个能运行、能测试、能讲清楚工程过程的监控系统。

本项目不是完整企业级监控产品，而是课程设计级可运行系统：它保留企业监控平台的核心思想，如 Agent 纳管、主机指标、历史趋势、服务组件、告警 ACK、团队协作和文档追踪，同时控制部署规模，适合 5 人小组、少量真实电脑和答辩现场演示。

## 当前状态

截至当前版本，AegisMonitor 已完成课程设计 MVP 主链路：

- Python Agent 可以在 Windows 主机上采集 CPU、内存、TCP 连接数和服务进程，并向中心后端持续上报。
- Spring Boot 后端完成 Agent 注册、心跳、指标入库、历史指标查询、服务组件管理、告警生成和 ACK 确认。
- MySQL 持久化保存主机、Agent、指标、服务和告警数据。
- Vue 3 前端完成总览、主机列表、主机详情、历史指标曲线、服务组件页和告警中心。
- 系统支持真实主机和模拟主机并存，适合答辩时展示“真实接入 + 稳定兜底数据”。
- 已补齐多主机接入说明、测试记录、验收截图、最终答辩脚本和项目讲解手册。

## 功能概览

| 模块 | 已实现能力 | 答辩价值 |
| --- | --- | --- |
| Agent 纳管 | 注册、心跳、本地状态文件、持续运行脚本 | 证明系统不是静态页面，而是有真实主机接入 |
| 主机监控 | CPU、内存、TCP、最近心跳、真实/模拟标签 | 展示一体化监控平台的基础能力 |
| 历史趋势 | 主机详情页展示指标曲线和时间范围切换 | 体现监控系统对时间序列数据的处理 |
| 服务发现 | 识别 NGINX、MySQL、Spring Boot 等服务实例 | 从主机层扩展到组件层监控 |
| 告警中心 | 高 CPU 告警、OPEN/ACKED 状态、ACK 备注 | 形成“发现问题 -> 人工确认”的运维闭环 |
| 演示数据 | 一键初始化模拟主机、服务和告警 | 保证答辩现场稳定可展示 |
| 工程文档 | PRD、需求规格、概要设计、接口原型、测试记录、答辩手册 | 对应软件工程课程设计要求 |

## 技术栈

| 层次 | 技术 |
| --- | --- |
| Agent | Python, psutil, requests |
| 后端 | Java 17, Spring Boot, JDBC, JUnit |
| 数据库 | MySQL |
| 前端 | Vue 3, Vite, ECharts, Vitest |
| 协作 | GitHub Issues, Pull Requests, feature branches |

## 仓库结构

```text
agent/       Python Agent，负责采集并上报主机、指标和服务数据
backend/     Spring Boot 后端，负责 API、业务逻辑、告警判断和 MySQL 持久化
frontend/    Vue 前端，负责监控台页面、曲线图、服务组件和告警 ACK 交互
docs/        软件工程文档、演示步骤、测试记录、验收截图和答辩资料
```

## 快速启动

### 1. 准备数据库

本地需要 MySQL。先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS aegis_monitor DEFAULT CHARACTER SET utf8mb4;
```

复制后端本地配置：

```powershell
copy backend\application-local.example.yml backend\application-local.yml
```

编辑 `backend/application-local.yml`，填写本机 MySQL 用户名和密码。这个文件包含本地敏感信息，不能提交到 Git。

### 2. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

### 3. 启动前端

```powershell
cd frontend
npm install
npm.cmd run dev
```

前端默认地址：

```text
http://127.0.0.1:5173/hosts
```

### 4. 启动本机 Agent

单次上报：

```powershell
agent\run-once.cmd --config agent\agent.example.yml
```

持续上报：

```powershell
agent\run.cmd --config agent\agent.example.yml
```

持续运行后，主机详情页中的 CPU、内存、TCP 连接数、最近心跳和历史曲线会随着 Agent 上报刷新。

## 接入其他真实主机

最终演示推荐结构：

```text
中心电脑：
- MySQL
- Spring Boot Backend: 8080
- Vue Frontend: 5173
- 可选：本机 Agent

同学电脑：
- 只运行 Python Agent
- Agent 上报到中心电脑 Backend
```

基本步骤：

1. 中心电脑执行 `ipconfig`，找到局域网 IPv4 地址。
2. 确保 Windows 防火墙允许其他电脑访问中心电脑的 `8080` 端口。
3. 同学电脑复制 `agent/agent.example.yml` 为 `agent/agent.yml`。
4. 修改同学电脑的 `agent.yml`：

```yaml
server_url: http://中心电脑IP:8080/api
register_token: demo-register-token
host_alias: teammate-a-laptop
host_metric_interval_seconds: 5
heartbeat_interval_seconds: 10
service_discovery_interval_seconds: 30
state_file: .agent-state.json
```

5. 同学电脑运行：

```powershell
agent\run.cmd --config agent\agent.yml
```

如果复制过别人的 Agent 目录，请先删除 `agent/.agent-state.json`，否则会复用旧 `hostId`，导致多台电脑在平台中显示成同一台主机。

完整说明见：

- `docs/AegisMonitor/15-多主机接入与演示部署说明.md`

## 验证命令

Agent 测试：

```powershell
agent\test.cmd
```

后端测试：

```powershell
backend\test.cmd
```

前端测试与构建：

```powershell
cd frontend
npm.cmd test
npm.cmd run build
```

当前文档记录的自动化测试基线为：

| 模块 | 结果 |
| --- | --- |
| 后端 | 26 项通过，0 失败 |
| 前端 | 20 项通过，0 失败 |
| Agent | 18 项通过，0 失败 |
| 合计 | 64 项通过，0 失败 |

## 答辩阅读顺序

如果只是想快速理解项目并准备讲解，建议按下面顺序读：

1. `docs/AegisMonitor/18-项目讲解手册-答辩版.md`
2. `docs/AegisMonitor/16-最终答辩演示脚本.md`
3. `docs/AegisMonitor/17-测试记录与验收截图.md`
4. `docs/AegisMonitor/15-多主机接入与演示部署说明.md`
5. `docs/AegisMonitor/01-产品需求文档-PRD.md`
6. `docs/AegisMonitor/02-需求规格说明书.md`
7. `docs/AegisMonitor/08-概要设计说明书.md`
8. `docs/AegisMonitor/09-接口原型.md`
9. `docs/AegisMonitor/10-数据模型原型.md`
10. `docs/AegisMonitor/TDD进展记录.md`

其中 `18-项目讲解手册-答辩版.md` 是给答辩者看的总手册，覆盖项目构成、数据流、关键代码、演示路线、常见追问和边界说明。

## 团队协作流程

推荐每个队员从最新 `master` 创建自己的功能分支：

```powershell
git clone https://github.com/qixing1227/AegisMonitor.git
cd AegisMonitor
git checkout master
git pull
git checkout -b feature/team-xx-short-name
```

完成任务后：

```powershell
git status
git add 相关文件
git commit -m "描述本次改动"
git push origin feature/team-xx-short-name
```

然后在 GitHub 上创建 Pull Request，由组长 review 后合并到 `master`。

## GitHub Issues 对应任务

| Issue | 目标 | 当前说明 |
| --- | --- | --- |
| #1 TEAM-01 | 多真实主机接入与局域网演示部署交接 | 已形成接入说明和真实多机截图 |
| #2 TEAM-02 | 前端展示打磨，区分真实主机与模拟主机 | 已完成主机来源标签和展示优化 |
| #3 TEAM-03 | 告警中心与服务组件展示完善 | 已完成服务组件截图、告警 OPEN/ACKED 闭环 |
| #4 TEAM-04 | 测试、演示脚本与答辩交接包 | 已补齐测试记录、验收截图、答辩脚本和讲解手册 |

## 不要提交的文件

这些文件属于本地运行环境、缓存、构建产物或临时导出，不应提交：

- `backend/application-local.yml`
- `agent/agent.yml`
- `agent/.agent-state.json`
- `frontend/node_modules/`
- `frontend/dist/`
- `backend/build/`
- `backend/target/`
- `backend/dev-server*.log`
- `frontend/dev-server*.log`
- `projects/`
- 根目录临时导出的 PPT 文件

## 项目边界

本项目已经实现课程设计展示所需的核心闭环，但以下内容属于企业级扩展，不作为当前 MVP 必须交付：

- 登录认证和完整 RBAC 权限隔离。
- WebSocket 实时推送。
- Prometheus / InfluxDB 等专业时序数据库。
- 容器化部署、Nginx 反向代理和云服务器部署。
- 自动恢复告警、通知渠道、复杂规则引擎。

答辩时建议说明：当前版本选择 MySQL + 轮询 + Agent 上报，是为了在课程周期内优先保证核心链路可运行、可测试、可展示；企业级能力已在设计文档中作为扩展方向保留。
