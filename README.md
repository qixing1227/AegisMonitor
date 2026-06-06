# AegisMonitor

AegisMonitor 是一个面向课程设计的一体化监控平台 MVP。系统通过 Python Agent 主动上报主机心跳、资源指标和服务发现结果，Spring Boot 后端接收并管理数据，Vue 前端提供主机监控、服务组件和告警 ACK 闭环展示。

当前项目目标不是做完整企业级监控系统，而是在课程设计范围内交付一个能运行、能展示、能说明工程过程的监控平台。

## 当前状态

- 已完成主机注册、心跳、指标上报、服务发现上报。
- 已完成 Spring Boot 后端、MySQL 持久化、Vue 前端监控页面。
- 已完成主机列表、主机详情、服务组件、告警中心和 ACK 闭环。
- 已支持本机 Agent 持续上报，也支持局域网内其他电脑作为真实主机接入。
- 已创建团队协作 Issues，适合队员基于最新 `master` 开分支完善。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| Agent | Python |
| Backend | Java 17, Spring Boot |
| Database | MySQL |
| Frontend | Vue 3, Vite |
| Collaboration | GitHub Issues + feature branches |

## 目录结构

```text
agent/       Python Agent，负责采集和上报主机数据
backend/     Spring Boot 后端，负责 API、数据管理和业务逻辑
frontend/    Vue 前端，负责监控台展示
docs/        需求、设计、演示、测试和任务文档
```

## 快速启动

### 1. 后端配置

复制本地配置文件：

```powershell
copy backend\application-local.example.yml backend\application-local.yml
```

编辑 `backend/application-local.yml`，填写本机 MySQL 连接信息。该文件包含本地密码，不要提交到 Git。

确认 MySQL 中存在数据库：

```sql
CREATE DATABASE IF NOT EXISTS aegis_monitor DEFAULT CHARACTER SET utf8mb4;
```

### 2. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

如果本机没有全局 Maven，请使用已经安装好的 Maven 绝对路径，或参考 `backend/test.cmd`。

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

持续上报后，主机详情页中的 CPU、内存、TCP 连接数和最近心跳会周期性刷新。

## 接入其他同学电脑

最终演示推荐结构：

```text
你的电脑：
- MySQL
- Spring Boot Backend: 8080
- Vue Frontend: 5173

同学电脑：
- 只运行 Python Agent
- Agent 上报到你的电脑 Backend
```

操作步骤：

1. 在你的电脑执行 `ipconfig`，找到局域网 IPv4 地址。
2. 确保 Windows 防火墙允许其他电脑访问 `8080`。如果要让同学也打开前端页面，也允许访问 `5173`。
3. 同学电脑复制 `agent/agent.example.yml` 为 `agent/agent.yml`。
4. 修改同学电脑的 `agent.yml`：

```yaml
server_url: http://你的电脑IP:8080/api
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

注意：如果复制过别人的 Agent 目录，请先删除 `agent/.agent-state.json`，否则会复用别人的 `hostId`，导致多台电脑显示成同一台主机。

## 团队任务

请队员从最新 `master` 拉代码后，认领自己的 Issue，并创建 feature 分支开发。

| 队员 | Issue | 目标 |
| --- | --- | --- |
| 队员 1 | [#1 TEAM-01 多真实主机接入与局域网演示部署交接](https://github.com/qixing1227/AegisMonitor/issues/1) | 接入其他真实电脑，整理部署交接文档 |
| 队员 2 | [#2 TEAM-02 前端展示打磨：区分真实主机与模拟主机](https://github.com/qixing1227/AegisMonitor/issues/2) | 优化主机列表和详情页展示 |
| 队员 3 | [#3 TEAM-03 告警中心与服务组件展示完善](https://github.com/qixing1227/AegisMonitor/issues/3) | 完善告警 ACK 和服务组件展示 |
| 队员 4 | [#4 TEAM-04 测试、演示脚本与答辩交接包](https://github.com/qixing1227/AegisMonitor/issues/4) | 整理测试记录、截图和答辩脚本 |

## 新成员阅读顺序

建议不要一上来就改代码，先按顺序读这些文档：

1. `docs/AegisMonitor/00-项目决策记录.md`
2. `docs/AegisMonitor/01-产品需求文档-PRD.md`
3. `docs/AegisMonitor/02-需求规格说明书.md`
4. `docs/AegisMonitor/04-五人展示分工说明.md`
5. `docs/AegisMonitor/08-概要设计说明书.md`
6. `docs/AegisMonitor/09-接口原型.md`
7. `docs/AegisMonitor/10-数据模型原型.md`
8. `docs/AegisMonitor/11-页面原型与演示流.md`
9. `docs/AegisMonitor/12-本地端到端演示步骤.md`
10. `docs/AegisMonitor/TDD进展记录.md`

## 协作流程

推荐每个队员这样做：

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

然后在 GitHub 上创建 Pull Request，由组长合并到 `master`。

不要提交这些文件：

- `backend/application-local.yml`
- `agent/agent.yml`
- `agent/.agent-state.json`
- `frontend/node_modules/`
- `frontend/dist/`
- `backend/build/`
- `backend/target/`
- 日志文件和本地缓存

## 验证命令

Agent：

```powershell
agent\test.cmd
```

后端：

```powershell
backend\test.cmd
```

前端：

```powershell
cd frontend
npm.cmd test
npm.cmd run build
```

## GitHub 协作权限

如果队员需要直接推送 feature 分支到本仓库，需要仓库所有者邀请他们成为 collaborator：

1. 打开 GitHub 仓库：`qixing1227/AegisMonitor`
2. 进入 `Settings`
3. 进入 `Collaborators`
4. 点击 `Add people`
5. 输入队员 GitHub 用户名或邮箱
6. 权限选择 `Write`
7. 队员接受邀请后，即可推送自己的 feature 分支

如果暂时不邀请队员，也可以让他们 fork 仓库后从自己的 fork 提 PR。但课程设计小组协作更推荐直接邀请为 collaborator，流程更简单。
