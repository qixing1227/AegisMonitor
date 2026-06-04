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

## 4. 当前测试命令

Agent 测试：

```powershell
$env:PYTHONPATH='D:\qixing\Documents\软件工程 2\agent'
& 'C:\Users\QiXing\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' -m unittest discover -s agent\tests -v
```

后端领域测试：

```powershell
$sources = Get-ChildItem -Path 'backend\src\main\java','backend\src\test\java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d 'backend\build\test-classes' $sources
java -cp 'backend\build\test-classes' com.aegismonitor.backend.agent.AgentRegistryContractTest
```

## 5. 已验证的 zoom-out 风险

| 风险 | 当前状态 |
| --- | --- |
| Agent Secret 需要本地持久化 | 已通过 `AgentStateStore` 实现和测试 |
| Agent 请求需要携带独立凭证 | 注册、心跳、指标上报测试已覆盖 |
| 监听端口列表不适合高频写入 InfluxDB | Agent 侧仍按接口上报，后端存储策略待实现 |
| 告警事件去重 | 尚未实现，留到告警模块 TDD |
| 服务指标 serviceId 映射 | 尚未实现，留到服务识别 TDD |

## 6. 下一轮 TDD 建议

下一轮不要立刻做完整前端或拓扑。建议继续完成 Sprint 1 主链路：

1. Agent 真实主机基础信息采集。
2. Agent 真实 CPU、内存、磁盘、网络、TCP 采集适配。
3. 后端主机指标接收领域模型。
4. 后端指标写入端口抽象。
5. MySQL/InfluxDB 连接环境准备。

若要进入 Spring Boot/JUnit 正式后端测试，需要先解决 Maven 或 Gradle 环境。

