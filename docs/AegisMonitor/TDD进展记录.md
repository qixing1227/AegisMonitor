# AegisMonitor TDD 进展记录

## 1. 当前阶段

当前进入 `tdd` 阶段，范围限定为 Sprint 1 最小主链路：

1. Agent 配置读取。
2. Agent 注册。
3. Agent 身份本地持久化。
4. Agent 心跳。
5. Agent 主机指标上报。
6. 后端 Agent 注册与心跳领域逻辑。

本阶段遵循垂直切片原则：一个行为测试，最小实现，通过后再进入下一个行为。

## 2. 环境检查

已确认：

- 工作区当前从文档阶段进入代码阶段。
- Python 标准库可用。
- Java 11 可用。

受限项：

- Python 环境暂未安装 `pytest`、`psutil`、`requests`、`yaml`。
- 本机暂未发现 Maven。
- 本机暂未发现 Gradle。
- 本机暂未发现 Docker。

影响：

- Agent 当前测试使用 Python 标准库 `unittest`。
- Agent 当前 HTTP 上报使用 Python 标准库 `urllib`。
- Agent 暂未实现真实 `psutil` 指标采集。
- 后端暂不能运行 Spring Boot/JUnit 测试，因此先用 Java 标准编译器验证核心领域逻辑。

## 3. 已完成 RED→GREEN 循环

### 3.1 Agent 配置读取

行为：

- Agent 能从 `agent.yml` 读取服务端地址、注册 Token、主机别名和采集周期。

测试：

- `agent/tests/test_config.py`

实现：

- `agent/aegis_agent/config.py`

结论：

- 已 GREEN。

### 3.2 Agent 身份持久化

行为：

- Agent 注册后能保存 `agentId`、`hostId` 和 `agentSecret`。
- Agent 重启后可从本地状态文件恢复身份。

测试：

- `agent/tests/test_identity.py`

实现：

- `agent/aegis_agent/identity.py`

结论：

- 已 GREEN。

### 3.3 Agent 注册

行为：

- Agent 使用注册 Token 调用后端注册接口。
- 后端返回身份后，Agent 将身份保存到本地状态文件。

测试：

- `agent/tests/test_registration.py`

实现：

- `agent/aegis_agent/host.py`
- `agent/aegis_agent/registration.py`
- `agent/aegis_agent/http_client.py`

结论：

- 已 GREEN。

### 3.4 Agent 心跳

行为：

- 已注册 Agent 使用 `agentId` 和 `agentSecret` 调用后端心跳接口。
- 请求体包含 `agentId`、`hostId`、`status` 和 `reportedAt`。

测试：

- `agent/tests/test_heartbeat.py`

实现：

- `agent/aegis_agent/heartbeat.py`

结论：

- 已 GREEN。

### 3.5 Agent 主机指标上报

行为：

- Agent 按接口原型上报 CPU、内存、磁盘、网络和 TCP 指标。
- 上报请求携带 `agentId` 和 `agentSecret`。

测试：

- `agent/tests/test_host_metrics.py`

实现：

- `agent/aegis_agent/metrics.py`

结论：

- 已 GREEN。

### 3.6 后端 Agent 注册与心跳领域逻辑

行为：

- 后端使用注册 Token 接受 Agent 注册。
- 后端创建 Host ID、Agent ID 和 Agent Secret。
- 后端接受合法 Agent 心跳并更新状态。
- 后端拒绝错误注册 Token。

测试：

- `backend/src/test/java/com/aegismonitor/backend/agent/AgentRegistryContractTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/agent/AgentRegistry.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentRegistrationRequest.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentRegistrationResult.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentHeartbeatRequest.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentStatus.java`

结论：

- 已 GREEN。

### 3.7 后端测试入口

行为：

- 后端领域测试可以通过一个稳定入口编译并运行。
- 测试入口会自动发现并运行所有 `*ContractTest`。
- 编译失败或任一 contract test 失败时，命令返回失败状态。

测试入口：

- `backend/test.cmd`
- `backend/test.ps1`

结论：

- 已 GREEN。

### 3.8 后端主机指标接收领域逻辑

行为：

- 后端接收 HostMetricReport。
- CPU 使用率、内存使用率、TCP 连接数作为最新指标点保存。
- TCP 监听端口列表作为主机运行快照保存。
- 该设计落实了 zoom-out 中“监听端口列表不适合高频写入 InfluxDB”的边界判断。

测试：

- `backend/src/test/java/com/aegismonitor/backend/metrics/HostMetricIngestionContractTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/metrics/CpuSample.java`
- `backend/src/main/java/com/aegismonitor/backend/metrics/MemorySample.java`
- `backend/src/main/java/com/aegismonitor/backend/metrics/TcpSample.java`
- `backend/src/main/java/com/aegismonitor/backend/metrics/HostMetricReport.java`
- `backend/src/main/java/com/aegismonitor/backend/metrics/HostMetricPoint.java`
- `backend/src/main/java/com/aegismonitor/backend/metrics/HostRuntimeSnapshot.java`
- `backend/src/main/java/com/aegismonitor/backend/metrics/HostMetricIngestionService.java`

结论：

- 已 GREEN。

### 3.9 Agent 真实主机基础信息采集

行为：

- Agent 能采集可用于注册的真实主机基础信息。
- 采集结果包含主机名、IP、操作系统、系统版本、CPU 核数、内存总量、启动时间和 Agent 版本。

测试：

- `agent/tests/test_host_collector.py`

实现：

- `agent/aegis_agent/host_collector.py`

结论：

- 已 GREEN。

### 3.10 Agent psutil 指标采集适配器

行为：

- Agent 能从 psutil-like provider 采集 CPU、内存、磁盘、网络和 TCP 指标。
- 采集结果转换为现有 `HostMetricSnapshot`，可直接复用主机指标上报逻辑。
- 测试使用 fake provider，不依赖本机是否已安装 `psutil`。

测试：

- `agent/tests/test_metric_collector.py`

实现：

- `agent/aegis_agent/metric_collector.py`
- `agent/requirements.txt`

结论：

- 已 GREEN。

### 3.11 Agent 测试入口

行为：

- Agent 测试可以通过一个稳定入口运行。

测试入口：

- `agent/test.cmd`

结论：

- 已 GREEN。

### 3.12 Agent 运行周期编排

行为：

- Agent 首次运行时能够自动注册、持久化身份、发送心跳并上报主机指标。
- Agent 重启后如果本地已有身份，不重复注册，只继续心跳和指标上报。
- 当配置了服务发现采集器时，`run_once` 会追加服务发现结果上报。

测试：

- `agent/tests/test_runtime.py`

实现：

- `agent/aegis_agent/runtime.py`

结论：

- 已 GREEN。

### 3.13 Agent 服务发现识别

行为：

- Agent 能从 psutil-like 的进程列表和 TCP 监听连接中识别 Spring Boot、MySQL、Redis、Nginx 和 Node.js。
- 服务发现结果包含服务名、技术栈类型、进程名、PID、监听端口、运行状态和命令行。
- 普通无关进程不会被识别为受管服务。

测试：

- `agent/tests/test_service_discovery.py`

实现：

- `agent/aegis_agent/service_discovery.py`

结论：

- 已 GREEN。

### 3.14 Agent 服务发现结果上报

行为：

- Agent 使用 `agentId` 和 `agentSecret` 调用 `/api/services/report`。
- 上报请求体遵守接口原型中的 `agentId`、`hostId`、`reportedAt` 和 `services` 结构。

测试：

- `agent/tests/test_service_report.py`

实现：

- `agent/aegis_agent/service_discovery.py`

结论：

- 已 GREEN。

### 3.15 后端服务清单 upsert 领域逻辑

行为：

- 后端接收服务发现报告后，按 `hostId + stackType + serviceName` 维护最新服务清单。
- 同一服务再次上报时更新 PID、端口和最后发现时间，而不是追加重复服务。
- 该逻辑为后续 `/api/services/report` Controller 和 MySQL 持久化提供领域核心。

测试：

- `backend/src/test/java/com/aegismonitor/backend/services/ServiceInventoryContractTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/services/DiscoveredServiceReport.java`
- `backend/src/main/java/com/aegismonitor/backend/services/ServiceDiscoveryReport.java`
- `backend/src/main/java/com/aegismonitor/backend/services/ServiceInstance.java`
- `backend/src/main/java/com/aegismonitor/backend/services/ServiceInventory.java`

结论：

- 已 GREEN。

### 3.16 Spring Boot 后端骨架

行为：

- 后端已从纯 Java 领域逻辑推进到 Spring Boot Maven 工程。
- Spring Boot 应用入口能够被测试上下文加载。
- Maven 统一管理 Spring Web、Validation、MySQL Driver 和 Spring Boot Test 依赖。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/AgentApiSpringTest.java`

实现：

- `backend/pom.xml`
- `backend/src/main/java/com/aegismonitor/backend/AegisMonitorApplication.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`
- `backend/src/main/java/com/aegismonitor/backend/api/ApiResponse.java`

结论：

- 已 GREEN。

### 3.17 Agent 注册与心跳 HTTP Controller

行为：

- `POST /api/agents/register` 能接收 Agent 注册请求，校验注册 Token，并返回 `agentId`、`hostId`、`agentSecret` 和审批状态。
- `POST /api/agents/heartbeat` 能接收已注册 Agent 的心跳请求，并返回服务端时间。
- HTTP 层复用已有 `AgentRegistry` 领域逻辑。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/AgentApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/agent/AgentController.java`

结论：

- 已 GREEN。

### 3.18 主机指标 HTTP Controller

行为：

- `POST /api/metrics/host` 能接收 Agent 上报的 CPU、内存、磁盘、网络和 TCP 指标请求。
- 当前 Spring Controller 将高频指标中的 CPU、内存和 TCP 摘要交给已有 `HostMetricIngestionService`。
- 接口返回 `written=true` 和 `generatedAlertCount=0`，为后续告警 TDD 留接口位置。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/HostMetricApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/metrics/HostMetricController.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`

结论：

- 已 GREEN。

### 3.19 服务发现结果 HTTP Controller

行为：

- `POST /api/services/report` 能接收 Agent 服务发现结果。
- 后端按服务清单 upsert 规则返回 `upsertedCount`。
- HTTP 层复用已有 `ServiceInventory` 领域逻辑。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/ServiceReportApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/services/ServiceReportController.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`

结论：

- 已 GREEN。

### 3.20 后端统一测试入口升级

行为：

- `backend/test.cmd` 在存在 `pom.xml` 时先运行 Maven/Spring Boot 测试。
- Maven 测试通过后继续运行已有 `*ContractTest` main-method 领域测试。
- 保留无 Maven 场景下的旧 javac fallback。

测试入口：

- `backend/test.cmd`
- `backend/test.ps1`

结论：

- 已 GREEN。

### 3.21 本地数据库配置与测试隔离

行为：

- Spring Boot 主配置只提交通用配置和占位变量。
- 本地 MySQL 密码放在被 `.gitignore` 忽略的 `backend/application-local.yml`。
- 后端自动测试使用 H2 内存库，不依赖本机 MySQL 是否启动，也不会读取本地密码。

实现：

- `backend/src/main/resources/application.yml`
- `backend/application-local.example.yml`
- `backend/src/test/resources/application.yml`
- `.gitignore`

结论：

- 已 GREEN。

### 3.22 Agent JDBC Repository

行为：

- 已注册 Agent 能保存到 `agents` 表。
- 后端能按 `agentId` 查询 Agent 身份和主机信息。
- Agent 心跳能更新状态和最后心跳时间。

测试：

- `backend/src/test/java/com/aegismonitor/backend/agent/JdbcAgentRepositoryTest.java`

实现：

- `backend/src/main/resources/schema.sql`
- `backend/src/test/resources/schema.sql`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentRecord.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/JdbcAgentRepository.java`

结论：

- 已 GREEN。

### 3.23 Agent 列表查询与 Registry 持久化

行为：

- `GET /api/agents` 能返回运维大屏需要的主机列表。
- 列表包含 Agent ID、Host ID、主机名、别名、IP、系统信息、资源规格、状态和最后心跳时间。
- 列表响应不会泄露 `agentSecret`。
- `AgentRegistry` 依赖 `AgentRepository`，Spring 环境使用 JDBC 仓储，纯领域测试继续使用内存仓储。
- Registry 重新创建后仍能从同一个仓储查到已注册 Agent，验证了最小持久化闭环。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/AgentApiSpringTest.java`
- `backend/src/test/java/com/aegismonitor/backend/agent/PersistentAgentRegistryTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/agent/AgentController.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentRegistry.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/AgentSummary.java`
- `backend/src/main/java/com/aegismonitor/backend/agent/InMemoryAgentRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`

结论：

- 已 GREEN。

### 3.24 告警规则、事件生成与 ACK 领域逻辑

行为：

- 运维配置 CPU 高阈值规则后，主机 CPU 使用率超过阈值会生成告警事件。
- 同一主机同一规则已有活跃告警时，不重复生成新事件，避免大屏刷屏。
- 运维可以确认告警 ACK，并记录处理人、处理时间和处理备注。

测试：

- `backend/src/test/java/com/aegismonitor/backend/alerts/AlertServiceContractTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/alerts/AlertRule.java`
- `backend/src/main/java/com/aegismonitor/backend/alerts/AlertEvent.java`
- `backend/src/main/java/com/aegismonitor/backend/alerts/AlertService.java`

结论：

- 已 GREEN。

### 3.25 主机指标上报触发告警

行为：

- `POST /api/metrics/host` 接收主机指标后，会把最新指标交给 `AlertService` 评估。
- CPU 使用率低于默认阈值时，接口返回 `generatedAlertCount=0`。
- CPU 使用率超过默认阈值时，接口返回 `generatedAlertCount=1`。
- 默认 CPU 高阈值通过 Spring 配置提供，课程演示默认值为 `80.0`。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/HostMetricApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/metrics/HostMetricController.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`

结论：

- 已 GREEN。

### 3.26 告警列表查询与 HTTP ACK

行为：

- `GET /api/alerts` 能返回由指标上报生成的告警列表，支撑运维大屏告警面板。
- `POST /api/alerts/{eventId}/ack` 能确认告警，并记录处理人、处理时间和处理备注。
- ACK 后再次查询告警列表，告警状态变为 `ACKED`。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/AlertApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/alerts/AlertController.java`
- `backend/src/main/java/com/aegismonitor/backend/alerts/AlertService.java`

结论：

- 已 GREEN。

### 3.27 告警事件 JDBC 持久化

行为：

- 告警事件生成后写入 `alert_events` 表。
- 告警 ACK 后更新同一条事件的状态、处理人、处理时间和处理备注。
- 重新创建 `AlertService` 后，仍能从 JDBC 仓储查询到已 ACK 的告警事件。
- Spring 环境使用 JDBC 告警仓储，纯领域测试继续使用内存仓储。

测试：

- `backend/src/test/java/com/aegismonitor/backend/alerts/PersistentAlertServiceTest.java`

实现：

- `backend/src/main/resources/schema.sql`
- `backend/src/test/resources/schema.sql`
- `backend/src/main/java/com/aegismonitor/backend/alerts/AlertRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/alerts/InMemoryAlertRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/alerts/JdbcAlertRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/alerts/AlertService.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`

结论：

- 已 GREEN。

### 3.28 后端测试临时目录隔离

行为：

- 当 C 盘临时目录空间不足时，`backend/test.cmd` 仍能运行后端测试。
- `backend/test.ps1` 会将 `TEMP` 和 `TMP` 指向工作区内的 `backend/build/temp`。
- 该目录属于构建产物，已由现有 `.gitignore` 忽略。

实现：

- `backend/test.ps1`

结论：

- 已 GREEN。

### 3.29 服务清单列表查询 HTTP 接口

行为：

- Agent 通过 `POST /api/services/report` 上报服务发现结果后，运维大屏可通过 `GET /api/services?hostId=...` 查询服务清单。
- 查询结果包含主机 ID、服务名、技术栈类型、进程名、PID、监听端口、状态、命令行和最后发现时间。
- HTTP 响应使用专门的 response record，不直接暴露领域对象。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/ServiceReportApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/services/ServiceReportController.java`

结论：

- 已 GREEN。

### 3.30 服务清单 JDBC 持久化

行为：

- 服务发现上报后写入 `service_instances` 表。
- 同一主机、同一技术栈、同一服务名再次上报时更新最新 PID、端口、状态、命令行和最后发现时间。
- 重新创建 `ServiceInventory` 后，仍能从 JDBC 仓储查询到最新服务清单。
- Spring 环境使用 JDBC 服务仓储，纯领域测试继续使用内存仓储。

测试：

- `backend/src/test/java/com/aegismonitor/backend/services/PersistentServiceInventoryTest.java`

实现：

- `backend/src/main/resources/schema.sql`
- `backend/src/test/resources/schema.sql`
- `backend/src/main/java/com/aegismonitor/backend/services/ServiceRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/services/InMemoryServiceRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/services/JdbcServiceRepository.java`
- `backend/src/main/java/com/aegismonitor/backend/services/ServiceInventory.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`

结论：

- 已 GREEN。

### 3.31 主机最新指标查询 HTTP 接口

行为：

- Agent 连续通过 `POST /api/metrics/host` 上报同一主机指标后，运维大屏可通过 `GET /api/metrics/host/latest?hostId=...` 查询最新快照。
- 查询结果返回主机 ID、上报时间、CPU 使用率、内存使用率和 TCP 连接数。
- HTTP 响应使用专门的 response record，不直接暴露领域对象。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/HostMetricApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/metrics/HostMetricController.java`

结论：

- 已 GREEN。

### 3.32 Agent run-once CLI 入口

行为：

- Agent 提供 `run-once` 命令行入口，可加载配置并执行一次注册、心跳、主机指标上报和服务发现上报。
- 命令执行成功后，输出本次使用的 `agentId` 和 `hostId`，便于答辩现场确认 Agent 已接入。
- CLI 入口复用既有 `AgentRuntime`，不重新实现注册、心跳或采集逻辑。

测试：

- `agent/tests/test_cli.py`

实现：

- `agent/aegis_agent/cli.py`

结论：

- 已 GREEN。

### 3.33 Agent Windows 演示脚本入口

行为：

- Agent 提供 `agent/run-once.cmd`，Windows 答辩现场可通过批处理脚本调用 `python -m aegis_agent.cli run-once`。
- 脚本会设置 `PYTHONPATH`，确保从项目目录运行时能找到 `aegis_agent` 包。
- 脚本支持通过 `AEGIS_AGENT_PYTHON` 指定 Python 解释器，便于测试环境和本机环境复用同一入口。

测试：

- `agent/tests/test_cli.py`

实现：

- `agent/run-once.cmd`

结论：

- 已 GREEN。

### 3.34 Agent 示例配置

行为：

- 仓库提供 `agent/agent.example.yml`，可作为答辩现场 `agent.yml` 的复制模板。
- 示例配置默认连接 `http://localhost:8080/api`，使用课程设计演示 Token，并把状态文件写入 Agent 目录下的 `.agent-state.json`。
- 自动化测试会直接加载示例配置，避免示例文件和真实配置解析规则脱节。

测试：

- `agent/tests/test_config.py`

实现：

- `agent/agent.example.yml`
- `.gitignore`

结论：

- 已 GREEN。

### 3.35 本地 MySQL 端到端冒烟

行为：

- 使用本地 ignored 的 `backend/application-local.yml` 启动 Spring Boot 后端，并连接 MySQL `aegis_monitor` 数据库。
- 使用 `agent/run-once.cmd --config agent/agent.example.yml` 执行一次 Agent 注册、心跳、主机指标上报和服务发现上报。
- 冒烟后通过 HTTP 接口查询主机列表、最新主机指标、服务清单和告警列表。

本次验证结果：

- 后端成功启动并连接 MySQL。
- Agent 输出 `run-once completed`，返回 `agentId=agt_001` 和 `hostId=host_001`。
- `GET /api/agents` 返回 1 台主机。
- `GET /api/metrics/host/latest?hostId=host_001` 返回最新 CPU 和内存指标。
- `GET /api/services?hostId=host_001` 返回 3 个服务实例。
- `GET /api/alerts` 返回 0 条告警。

文档：

- `docs/AegisMonitor/12-本地端到端演示步骤.md`

结论：

- 已通过真实本地冒烟。

### 3.36 演示数据 seed HTTP 接口

行为：

- `POST /api/demo/seed` 能为前端演示创建 3 台模拟主机，补齐课程设计答辩时的多主机展示规模。
- seed 接口可重复执行；重复调用不会无限追加主机、服务或告警。
- 当 `includeServices=true` 时，接口会为模拟 Web、应用、数据库主机创建 NGINX、Spring Boot、Redis、MySQL 等服务实例。
- 当 `includeAlerts=true` 时，接口会创建一条数据库主机 CPU 高危告警，便于前端稳定演示告警列表与 ACK 闭环。

测试：

- `backend/src/test/java/com/aegismonitor/backend/api/DemoSeedApiSpringTest.java`

实现：

- `backend/src/main/java/com/aegismonitor/backend/api/demo/DemoSeedController.java`
- `backend/src/main/java/com/aegismonitor/backend/demo/DemoDataSeeder.java`
- `backend/src/main/java/com/aegismonitor/backend/demo/DemoSeedResult.java`
- `backend/src/main/java/com/aegismonitor/backend/BackendConfiguration.java`

结论：

- 已 GREEN。

### 3.37 前端 FE-0001 主机监控台 tracer bullet

行为：

- `frontend` 已从 prototype 进入真实 Vue 入口，首页直接显示主机监控台，而不是落地页。
- 前端 API adapter 通过 `GET /api/agents` 读取后端主机列表，并映射成页面 Host 模型。
- 页面模型不会把 `agentSecret` 泄露到前端展示层。
- 主机监控台能展示主机总数、在线主机数、模拟主机数和主机表格。
- 页面状态层支持 loading、empty 和 error 三种基础状态。

测试：

- `frontend/src/api/aegisApi.test.js`
- `frontend/src/dashboard/hostDashboardStore.test.js`

实现：

- `frontend/src/api/aegisApi.js`
- `frontend/src/dashboard/hostDashboardStore.js`
- `frontend/src/dashboard/HomeDashboard.vue`
- `frontend/src/App.vue`
- `frontend/src/styles.css`

结论：

- 已 GREEN。

### 3.38 前端 FE-0002 最新主机指标快照

行为：

- 前端 API adapter 能通过 `GET /api/metrics/host/latest?hostId=...` 读取选中主机的最新指标快照。
- 主机监控台加载主机列表后，会自动选中第一台主机并读取其最新 CPU、内存和 TCP 连接数。
- 首页第一屏新增最新指标快照区域，展示选中主机、最近上报时间、CPU 使用率、内存使用率和 TCP 连接数。
- 当指标暂未上报时，页面以 `--` 和“等待 Agent 上报最新指标”表达空状态，避免误显示为 0。

测试：

- `frontend/src/api/aegisApi.test.js`
- `frontend/src/dashboard/hostDashboardStore.test.js`

实现：

- `frontend/src/api/aegisApi.js`
- `frontend/src/dashboard/hostDashboardStore.js`
- `frontend/src/dashboard/HomeDashboard.vue`
- `frontend/src/styles.css`

结论：

- 已 GREEN。

### 3.39 前端 FE-0003 主机列表到详情闭环

行为：

- 前端接入 Vue Router，`/` 自动跳转到 `/hosts`，主机详情页使用 `/hosts/:hostId`。
- 主机列表中的主机名和“详情”入口可跳转到单台主机详情。
- 主机详情页复用已加载的 Agent 列表和最新指标快照，不要求后端新增 `/api/hosts/{hostId}`。
- 直接访问 `/hosts/:hostId` 时，前端会重新拉取 `/api/agents`，找到对应主机后展示基础信息和最新指标。
- 当 `hostId` 不存在时，页面展示“主机不存在或尚未接入”，并避免请求不存在主机的指标快照。

测试：

- `frontend/src/dashboard/hostDashboardStore.test.js`

实现：

- `frontend/src/router.js`
- `frontend/src/main.js`
- `frontend/src/App.vue`
- `frontend/src/dashboard/dashboardStore.js`
- `frontend/src/dashboard/HomeDashboard.vue`
- `frontend/src/dashboard/HostDetailPage.vue`
- `frontend/src/dashboard/hostDashboardStore.js`
- `frontend/src/styles.css`

结论：

- 已 GREEN。

### 3.40 前端 FE-0004 服务组件列表

行为：

- 前端 API adapter 能通过 `GET /api/services?hostId=...` 读取选中主机的服务清单。
- adapter 将端口数组转换为 `portsText`，避免页面表格直接渲染数组造成挤压。
- dashboard store 会在加载默认主机、切换主机时同步读取该主机服务清单。
- `/services` 页面可从主机列表选择主机，并展示服务名、技术栈、进程名、PID、端口、状态和最后发现时间。
- 当选中主机暂无服务时，页面展示“等待 Agent 服务发现上报”。

测试：

- `frontend/src/api/aegisApi.test.js`
- `frontend/src/dashboard/hostDashboardStore.test.js`

实现：

- `frontend/src/api/aegisApi.js`
- `frontend/src/dashboard/hostDashboardStore.js`
- `frontend/src/dashboard/ServicesPage.vue`
- `frontend/src/router.js`
- `frontend/src/dashboard/HomeDashboard.vue`
- `frontend/src/dashboard/HostDetailPage.vue`
- `frontend/src/styles.css`

结论：

- 已 GREEN。

### 3.41 前端 FE-0005 告警中心与 ACK 闭环

行为：

- 前端 API adapter 能通过 `GET /api/alerts` 读取告警事件，并映射为页面可直接使用的 `AlertEvent`。
- adapter 能通过 `POST /api/alerts/{eventId}/ack` 提交 `acknowledgedBy`、`acknowledgedAt`、`ackNote`，并将响应事件映射为已确认状态。
- dashboard store 暴露 `alerts`、`alertsEmpty`、`loadAlerts()` 和 `ackAlert()`，ACK 成功后会更新当前告警列表中的对应事件。
- 新增 `/alerts` 告警中心页面，展示告警总数、待处理数、已确认数，以及事件表格。
- 未确认告警显示 ACK 操作；ACK 面板收集确认人、确认时间和处理备注，并做基础必填校验。
- 侧边栏“告警中心”已从占位禁用入口改为真实路由入口。

测试：

- `frontend/src/api/aegisApi.test.js`
- `frontend/src/dashboard/hostDashboardStore.test.js`

实现：

- `frontend/src/api/aegisApi.js`
- `frontend/src/dashboard/hostDashboardStore.js`
- `frontend/src/dashboard/AlertsPage.vue`
- `frontend/src/router.js`
- `frontend/src/dashboard/HomeDashboard.vue`
- `frontend/src/dashboard/HostDetailPage.vue`
- `frontend/src/dashboard/ServicesPage.vue`
- `frontend/src/styles.css`

验证：

- `npm.cmd test`：13 项通过。
- `npm.cmd run build`：Vite production build 通过。
- `http://127.0.0.1:5173/alerts`：HTTP 200。
- Browser 后台检查：能看到 `/alerts` 页面壳、导航、统计卡和表头；当前未启动 Spring Boot 后端时，Vite proxy 会在页面显示 API 500，这属于运行环境未连后端，不是前端路由或构建错误。

结论：

- 已 GREEN。

### 3.42 前端 FE-0006 自动刷新与接口错误治理

行为：

- 新增可测试的 `refreshLoop`，页面可以启动定时刷新，并在卸载时清理定时器。
- 主机总览和主机详情默认 5 秒刷新。
- 服务组件和告警中心默认 10 秒刷新。
- 服务组件刷新保留当前选中主机，不会因为自动刷新跳回第一台主机。
- 主机切换或刷新失败时，store 会显示错误提示，并保留上一份可用主机快照，避免页面闪空。
- 手动刷新按钮继续可用，并复用当前页面的刷新行为。

测试：

- `frontend/src/dashboard/refreshLoop.test.js`
- `frontend/src/dashboard/hostDashboardStore.test.js`

实现：

- `frontend/src/dashboard/refreshLoop.js`
- `frontend/src/dashboard/HomeDashboard.vue`
- `frontend/src/dashboard/HostDetailPage.vue`
- `frontend/src/dashboard/ServicesPage.vue`
- `frontend/src/dashboard/AlertsPage.vue`
- `frontend/src/dashboard/hostDashboardStore.js`
- `frontend/package.json`

验证：

- `npm.cmd test`：15 项通过。
- `npm.cmd run build`：Vite production build 通过。
- `http://127.0.0.1:5173/services`：HTTP 200。
- `http://127.0.0.1:5173/alerts`：HTTP 200。
- Browser 后台检查：服务页和告警页页面标题可见，控制台无错误日志。

结论：

- 已 GREEN。

### 3.43 前端 FE-0007 演示数据入口与答辩展示流

行为：

- 前端 API adapter 能通过 `POST /api/demo/seed` 初始化 3 台模拟主机、服务组件和告警事件。
- dashboard store 暴露 `seedDemoData()`，初始化成功后会刷新主机列表和告警列表，并保存本次 seed 结果用于页面提示。
- 主机监控首页新增开发/演示模式下可见的“初始化演示数据”按钮。
- 首页第一屏展示主机总数、在线主机、模拟主机和待处理告警，支撑答辩开场讲解。
- 本地端到端演示文档补充前端启动命令和答辩点击路径：主机监控 -> 主机详情 -> 服务组件 -> 告警中心 ACK。

测试：

- `frontend/src/api/aegisApi.test.js`
- `frontend/src/dashboard/hostDashboardStore.test.js`

实现：

- `frontend/src/api/aegisApi.js`
- `frontend/src/dashboard/hostDashboardStore.js`
- `frontend/src/dashboard/HomeDashboard.vue`
- `frontend/src/styles.css`
- `docs/AegisMonitor/12-本地端到端演示步骤.md`
- `docs/AegisMonitor/13-前端对接任务-to-issues.md`

验证：

- `npm.cmd test`：17 项通过。
- `npm.cmd run build`：Vite production build 通过。
- `http://127.0.0.1:5173/hosts`：HTTP 200。
- `http://127.0.0.1:5173/alerts`：HTTP 200。
- Browser 后台检查：首页标题、初始化演示数据按钮、待处理告警卡可见，控制台无错误日志。

结论：

- 已 GREEN。

### 3.44 前端 FE-0008 MVP 边界与暂缓项归档

行为：

- 使用 zoom-out 视角重新确认第一版交付边界：优先保证 Agent、后端、MySQL、Vue 运维大屏和告警 ACK 主链路。
- 明确登录认证、用户管理、真实 RBAC、告警规则编辑、历史曲线、服务拓扑、WebSocket 推送和部署运维暂不进入第一版代码。
- 新增 MVP 边界归档文档，说明暂缓原因、后续扩展方式和答辩话术。
- 前端导航只保留主机监控、服务组件、告警中心三个可工作入口，避免展示不可用页面。
- `FE-0001` 到 `FE-0008` 已全部完成，前端主链路进入提交前整理阶段。

实现：

- `docs/AegisMonitor/14-MVP边界与暂缓项归档.md`
- `docs/AegisMonitor/13-前端对接任务-to-issues.md`

验证：

- 文档已归档第一版暂缓范围。
- 页面导航范围已和当前可运行前端一致。
- 答辩话术已覆盖“课程设计 MVP 已实现主链路，企业级权限、历史曲线和拓扑作为扩展设计”。

结论：

- 已 GREEN。

## 4. 当前测试命令

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

说明：

- 当前后端测试入口会同时运行 Spring Boot MockMvc 测试和纯 Java contract tests。
- Spring Boot 测试使用 H2 内存库；本地运行应用时可通过被忽略的 `backend/application-local.yml` 接入 MySQL。
- 后端测试脚本会把临时目录切到 `backend/build/temp`，避免 C 盘临时目录空间不足影响测试。
- 前端测试当前覆盖 API adapter 和 dashboard store，构建命令用于验证 Vue 页面模板与 Vite 打包。
- 在 Codex 沙盒中 Maven 访问依赖缓存可能需要用户授权；在你的正常 Windows 用户终端里可直接运行。

## 5. 已验证的 zoom-out 风险

| 风险 | 当前状态 |
| --- | --- |
| Agent Secret 需要本地持久化 | 已通过 `AgentStateStore` 实现和测试 |
| Agent 请求需要携带独立凭证 | 注册、心跳、指标上报、服务发现上报测试已覆盖 |
| 监听端口列表不适合高频写入 InfluxDB | 后端指标接收领域逻辑已将 `listeningPorts` 作为运行快照保存 |
| 告警事件去重 | 已在 `AlertService` 领域逻辑中实现同一主机同一规则的活跃告警去重 |
| 告警闭环需要可展示 | 已完成高 CPU 指标触发告警、告警列表查询、HTTP ACK 和处理备注 |
| 告警事件需要服务端持久化 | 已通过 `AlertRepository` + `JdbcAlertRepository` 接入 `alert_events` 表 |
| 服务指标 serviceId 映射 | 已收敛为服务清单按 `hostId + stackType + serviceName` upsert，并通过 `service_instances` 表持久化 |
| 后端领域逻辑需要接入真实 HTTP 层 | 已完成 Agent 注册、心跳、列表查询、主机指标上报、主机最新指标查询、服务发现结果、服务列表查询、告警查询与 ACK 9 条 Spring Boot Controller 竖切片 |
| Agent 注册信息需要服务端持久化 | 已通过 `AgentRepository` + `JdbcAgentRepository` 接入 `agents` 表 |
| 答辩现场需要稳定的 Agent 启动方式 | 已完成 `python -m aegis_agent.cli run-once` 和 `agent/run-once.cmd`，可一键执行一次上报链路 |
| 示例配置可能和代码解析规则脱节 | 已通过 `agent/tests/test_config.py` 直接加载 `agent/agent.example.yml` |
| 本地 MySQL 端到端链路可能只停留在单元测试 | 已完成 Spring Boot + MySQL + Agent run-once + HTTP 查询冒烟 |

## 6. 下一轮 TDD 建议

下一轮不要立刻做完整前端或拓扑。建议继续完成 Sprint 1 到 Sprint 2 的工程落地：

1. 开始前端前，整理一份后端接口对接清单，降低 Vue 对接返工。
2. 准备前端 Dashboard 原型，对接主机、主机最新指标、服务、告警接口。
3. 给后端补一个演示数据重置或模拟主机入口，方便答辩现场展示 5 台主机。
4. 再进入指标历史曲线前，先决定课程设计阶段是否只展示最新快照，避免过早引入 InfluxDB。
5. 整理课程设计报告中的测试用例表，把当前自动化测试和真实冒烟映射到验收标准。
