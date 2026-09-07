# Springboot-RPA

一个面向企业自动化运营场景的 RPA 管理平台。系统提供机器人、流程、任务、队列、触发器、日志、报表、凭据、AI、安全治理和权限管理等能力，采用 Spring Boot 与 Vue 3 前后端分离架构，支持本地开发及 Docker Compose 一键部署。

> 本文档中的截图来自真实 Docker 演示环境，数据均为模拟数据，不包含真实业务账号、密钥或客户信息。

## 系统预览

[![Springboot-RPA 全功能截图总览](docs/screenshots/00-overview.png)](docs/screenshots/00-overview.png)

演示环境信息：

| 项目 | 配置 |
| --- | --- |
| 前端地址 | `http://localhost:8088` |
| 后端地址 | `http://localhost:18080` |
| 演示账号 | `admin` |
| 演示密码 | `123456` |
| 数据库 | MySQL 8，宿主机端口 `3307` |
| 缓存 | Redis 7 |

## 核心能力

- 实时监控：展示运行任务、成功率、在线机器人、执行趋势、状态分布和实时告警。
- 任务调度：通过任务、队列和触发器组织自动化执行，可查看进度及运行结果。
- 机器人管理：管理机器人在线状态、分类、CPU、内存、任务量、成功率和健康度。
- 流程管理：维护流程版本、启停状态、执行统计，并通过可视化设计器编排流程。
- 日志与审计：查询执行日志、错误详情、死信任务、用户操作记录和风险等级。
- 数据与报表：查询采集及处理结果，分析执行数量、成功率、耗时和流程表现。
- 企业级安全：提供凭据保险箱、数据脱敏、水印、分布式锁和权限资源管理。
- 协作与通知：围绕任务、机器人和日志开展团队协作，并管理系统公告。
- AI 能力：提供 OCR、票据识别、表格识别、文档处理及大模型服务配置入口。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 2.7.18、Spring Security、JWT、Spring Data JPA |
| 前端 | Vue 3、Vite、Element Plus、ECharts、Axios、Vue Router |
| 数据 | MySQL 8、Redis 7 |
| 调度与通信 | Quartz、WebSocket |
| 部署 | Docker、Docker Compose、Nginx |

## Docker 一键部署

### 1. 环境准备

请先安装并启动 Git、Docker Desktop 或 Docker Engine，以及 Docker Compose v2。

### 2. 克隆项目

```bash
git clone https://github.com/lwxiaoye/Springboot-RPA.git
cd Springboot-RPA
```

### 3. 配置环境变量

Linux/macOS：

```bash
cp .env.example .env
```

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

建议至少检查以下配置。生产环境必须更换示例密码和 JWT 密钥：

```dotenv
BACKEND_PORT=18080
FRONTEND_PORT=8088
MYSQL_PORT=3307

DB_HOST=rpa-mysql
DB_PORT=3306
DB_NAME=rpa_system
DB_USERNAME=root
DB_PASSWORD=请替换为数据库密码
MYSQL_ROOT_PASSWORD=请替换为数据库密码

REDIS_HOST=rpa-redis
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_SECRET=请替换为至少32字节的随机字符串
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=http://localhost:8088
```

### 4. 构建并启动完整环境

```bash
docker compose --profile full up -d --build
```

该命令会启动以下服务：

| 服务 | 容器名 | 容器端口 | 演示环境宿主机端口 | 作用 |
| --- | --- | --- | --- | --- |
| 前端 | `rpa-frontend` | `80` | `8088` | Vue 页面与 Nginx API 代理 |
| 后端 | `rpa-backend` | `8080` | `18080` | Spring Boot REST API |
| 数据库 | `rpa-mysql` | `3306` | `3307` | 业务数据持久化 |
| 缓存 | `rpa-redis` | `6379` | `6379` | 缓存、锁及实时能力 |

### 5. 检查运行状态

```bash
docker compose ps
docker compose logs -f rpa-backend
```

容器启动完成后，浏览器访问 `http://localhost:8088`。如果没有修改端口，前端默认使用 `http://localhost`。

### 6. 停止或重新启动

```bash
# 停止服务并保留数据库卷
docker compose --profile full down

# 重启服务
docker compose --profile full restart

# 重新构建前后端
docker compose --profile full up -d --build
```

不要随意在 `down` 后添加 `-v`，否则 MySQL 和 Redis 数据卷会被删除。

## 本地开发

### 后端

```bash
mvn spring-boot:run
```

后端默认地址为 `http://localhost:8080`。本地数据库默认端口为 `3307`，可通过环境变量覆盖。

### 前端

```bash
npm install
npm run dev
```

前端默认地址为 `http://localhost:5173`，Vite 会将 `/api` 请求代理到后端。

### 构建与测试

```bash
mvn test
npm run build
```

## 登录与首次使用

1. 打开系统登录页。
2. 输入演示账号 `admin` 和密码 `123456`。
3. 登录后首先检查实时监控首页，确认机器人数量、任务成功率和实时任务流是否正常。
4. 生产环境部署后应立即修改管理员密码，并限制管理端访问来源。

[![登录页面](docs/screenshots/01-login.png)](docs/screenshots/01-login.png)

## 功能导航

| 模块 | 路由 | 主要用途 |
| --- | --- | --- |
| 实时监控 | `/dashboard` | 查看任务、机器人、趋势、告警和实时任务流 |
| 任务中心 | `/rpa/tasks` | 新建、筛选和跟踪自动化任务 |
| 任务队列 | `/rpa/queues` | 管理优先级、容量和待处理任务 |
| 触发器 | `/rpa/triggers` | 配置定时、周期和事件触发规则 |
| 脚本执行 | `/rpa/script` | 在线编辑及运行脚本 |
| 机器人管理 | `/rpa/robots` | 查看机器人状态、资源和执行表现 |
| 流程管理 | `/rpa/processes` | 维护流程、版本和发布状态 |
| 流程设计器 | `/rpa/process-designer` | 通过节点和连线编排流程 |
| 执行日志 | `/rpa/logs` | 查询任务执行记录及错误详情 |
| 审计日志 | `/rpa/audit` | 追踪用户操作、IP 和风险等级 |
| 数据查询 | `/rpa/data-query` | 查询采集、解析和处理后的业务数据 |
| 报表分析 | `/rpa/reports` | 分析数量、成功率、耗时和流程表现 |
| 凭据保险箱 | `/rpa/credentials` | 管理自动化账号凭据元数据 |
| AI 中心 | `/rpa/ai` | OCR、票据、表格和文档识别 |
| 系统设置 | `/rpa/settings` | 配置消息、存储、OCR 和大模型服务 |
| 数据脱敏 | `/rpa/masking` | 配置手机号、证件号、银行卡等脱敏规则 |
| 分布式锁 | `/rpa/locks` | 监控并发锁、等待时间和竞争情况 |
| 水印管理 | `/rpa/watermark-settings` | 配置敏感页面水印 |
| 协作中枢 | `/rpa/collaboration` | 围绕任务、机器人和日志协同处理 |
| 通知中心 | `/rpa/notifications` | 发布、阅读和统计系统公告 |
| 机器人健康 | `/rpa/robot-health` | 按健康、警告、危险和离线状态监控机器人 |
| 死信队列 | `/rpa/dead-letter` | 处理超过重试限制的失败任务 |
| 个人中心 | `/system/profile` | 查看账户资料和安全设置 |
| 用户管理 | `/system/users` | 管理用户、角色、部门和状态 |
| 角色管理 | `/system/roles` | 配置角色及权限集合 |
| 资源管理 | `/system/resources` | 管理菜单、按钮和接口权限资源 |

## 推荐演示流程

### 1. 实时监控

登录后先查看核心指标、执行趋势、状态分布和实时任务流。若出现失败任务，可继续前往执行日志或死信队列排查。

[![实时监控首页](docs/screenshots/02-dashboard.png)](docs/screenshots/02-dashboard.png)

### 2. 任务、队列与触发器

1. 在任务中心按名称、流程、机器人或状态筛选任务。
2. 查看任务优先级、当前进度和最近执行结果。
3. 在任务队列中确认容量与积压情况。
4. 在触发器中配置定时或事件规则，并检查最近和下次触发时间。

### 3. 机器人与流程

1. 在机器人管理中检查在线状态、CPU、内存、任务量和成功率。
2. 进入机器人详情查看单机执行历史和资源表现。
3. 在流程管理中查看版本、状态及执行统计。
4. 使用流程设计器拖入节点、配置参数、连接执行顺序并保存流程。

### 4. 日志、审计与失败处理

1. 通过执行日志定位失败任务和错误上下文。
2. 在死信队列检查失败原因和重试次数，修复问题后执行重试或标记解决。
3. 在审计日志中根据操作者、IP、动作和风险等级追溯关键操作。

### 5. 数据与安全治理

1. 在数据查询中选择业务数据类型和条件。
2. 在报表分析中观察成功率、耗时和流程表现。
3. 在凭据保险箱中维护凭据有效期，避免在代码中明文保存密码。
4. 使用数据脱敏和水印能力保护日志、报表和导出数据。
5. 通过分布式锁监控重复执行与资源竞争。

### 6. 用户与权限

1. 新建用户并分配角色。
2. 在角色管理中配置菜单和操作权限。
3. 在资源管理中维护菜单、按钮和接口资源。
4. 使用测试账号验证最小权限是否生效。

## 功能截图

点击图片可查看原图。

<table>
  <tr><td align="center"><a href="docs/screenshots/03-tasks.png"><img src="docs/screenshots/03-tasks.png" alt="任务中心"></a><br><b>任务中心</b></td><td align="center"><a href="docs/screenshots/04-queues.png"><img src="docs/screenshots/04-queues.png" alt="任务队列"></a><br><b>任务队列</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/05-triggers.png"><img src="docs/screenshots/05-triggers.png" alt="触发器"></a><br><b>触发器</b></td><td align="center"><a href="docs/screenshots/06-script-executor.png"><img src="docs/screenshots/06-script-executor.png" alt="脚本执行"></a><br><b>脚本执行</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/07-robots.png"><img src="docs/screenshots/07-robots.png" alt="机器人管理"></a><br><b>机器人管理</b></td><td align="center"><a href="docs/screenshots/08-processes.png"><img src="docs/screenshots/08-processes.png" alt="流程管理"></a><br><b>流程管理</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/09-process-designer.png"><img src="docs/screenshots/09-process-designer.png" alt="流程设计器"></a><br><b>流程设计器</b></td><td align="center"><a href="docs/screenshots/10-execution-logs.png"><img src="docs/screenshots/10-execution-logs.png" alt="执行日志"></a><br><b>执行日志</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/11-audit-log.png"><img src="docs/screenshots/11-audit-log.png" alt="审计日志"></a><br><b>审计日志</b></td><td align="center"><a href="docs/screenshots/12-data-query.png"><img src="docs/screenshots/12-data-query.png" alt="数据查询"></a><br><b>数据查询</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/13-report-analytics.png"><img src="docs/screenshots/13-report-analytics.png" alt="报表分析"></a><br><b>报表分析</b></td><td align="center"><a href="docs/screenshots/14-credential-vault.png"><img src="docs/screenshots/14-credential-vault.png" alt="凭据保险箱"></a><br><b>凭据保险箱</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/15-ai-center.png"><img src="docs/screenshots/15-ai-center.png" alt="AI 中心"></a><br><b>AI 中心</b></td><td align="center"><a href="docs/screenshots/16-system-settings.png"><img src="docs/screenshots/16-system-settings.png" alt="系统设置"></a><br><b>系统设置</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/17-data-masking.png"><img src="docs/screenshots/17-data-masking.png" alt="数据脱敏"></a><br><b>数据脱敏</b></td><td align="center"><a href="docs/screenshots/18-distributed-lock.png"><img src="docs/screenshots/18-distributed-lock.png" alt="分布式锁"></a><br><b>分布式锁</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/19-watermark-settings.png"><img src="docs/screenshots/19-watermark-settings.png" alt="水印管理"></a><br><b>水印管理</b></td><td align="center"><a href="docs/screenshots/20-collaboration-hub.png"><img src="docs/screenshots/20-collaboration-hub.png" alt="协作中枢"></a><br><b>协作中枢</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/21-notifications.png"><img src="docs/screenshots/21-notifications.png" alt="通知中心"></a><br><b>通知中心</b></td><td align="center"><a href="docs/screenshots/22-robot-health.png"><img src="docs/screenshots/22-robot-health.png" alt="机器人健康监控"></a><br><b>机器人健康监控</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/23-dead-letter-queue.png"><img src="docs/screenshots/23-dead-letter-queue.png" alt="死信队列"></a><br><b>死信队列</b></td><td align="center"><a href="docs/screenshots/24-user-profile.png"><img src="docs/screenshots/24-user-profile.png" alt="个人中心"></a><br><b>个人中心</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/25-user-management.png"><img src="docs/screenshots/25-user-management.png" alt="用户管理"></a><br><b>用户管理</b></td><td align="center"><a href="docs/screenshots/26-role-management.png"><img src="docs/screenshots/26-role-management.png" alt="角色管理"></a><br><b>角色管理</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/27-resource-management.png"><img src="docs/screenshots/27-resource-management.png" alt="资源管理"></a><br><b>资源管理</b></td><td align="center"><a href="docs/screenshots/28-robot-detail.png"><img src="docs/screenshots/28-robot-detail.png" alt="机器人详情"></a><br><b>机器人详情</b></td></tr>
  <tr><td align="center"><a href="docs/screenshots/29-process-detail.png"><img src="docs/screenshots/29-process-detail.png" alt="流程详情"></a><br><b>流程详情</b></td><td align="center"><a href="docs/screenshots/02-dashboard.png"><img src="docs/screenshots/02-dashboard.png" alt="实时监控"></a><br><b>实时监控</b></td></tr>
</table>

## 演示数据

演示环境预置了覆盖主要功能的大批量模拟数据。12 个核心展示表均超过 100 条，可直接演示分页、组合筛选、状态分布、趋势统计和异常处理：

| 数据类型 | 数量 | 示例内容 |
| --- | ---: | --- |
| 用户 | 128 | 管理员、运营、财务、数据、客服和风控等部门用户 |
| 机器人 | 128 | 财务票据、订单处理、客户服务、数据采集、报表和巡检机器人 |
| 流程 | 126 | 发票核验、订单同步、客户回访、日报、库存和合同流程 |
| 任务 | 132 | 运行中、成功、失败、等待、取消等多种状态 |
| 任务队列 | 4 | 财务、客户、工单和数据处理队列 |
| 触发器 | 126 | 定时、文件、Webhook 和条件触发示例 |
| 执行日志 | 141+ | 成功、运行中、警告、失败和超时等记录 |
| 机器人健康 | 128 | 健康、警告、危险和离线状态及资源指标 |
| 凭据元数据 | 125 | 账号、数据库、API、Token 和 SSH 等类型 |
| 审计日志 | 128 | 创建、更新、执行、导出、登录和删除操作 |
| 公告 | 125 | 维护、上线、报告、扩容、轮换和培训通知 |
| 发票数据 | 126 | 多企业、多票种、多状态和金额区间的业务结果 |
| 死信任务 | 124 | 超时、网络、校验、鉴权和目标繁忙等异常示例 |

如需重新填充演示数据，请在数据库表结构创建完成后导入对应演示 SQL。所有示例凭据均为占位内容，不能用于真实系统。

## 目录结构

```text
.
├── src/main/java/rpa              # 后端源码
├── src/main/resources             # 后端配置与 SQL
├── src/test/java/rpa              # 后端测试
├── src/views                      # Vue 页面
├── src/components                 # 前端组件
├── src/api                        # 前端接口封装
├── src/router                     # 前端路由
├── docs/screenshots               # 功能截图
├── public                         # 前端公共资源
├── pom.xml                        # Maven 配置
├── package.json                   # 前端依赖与脚本
├── docker-compose.yml             # Docker Compose 配置
├── Dockerfile                     # 后端镜像
├── Dockerfile.frontend            # 前端镜像
└── README.md                      # 项目说明
```

## 常用配置

- `src/main/resources/application.yml`：本地开发配置。
- `src/main/resources/application-prod.yml`：生产环境配置。
- `.env.example`：Docker 环境变量示例。
- `nginx.conf`：前端静态资源和 `/api` 反向代理配置。

## 常见问题

### 前端页面无法访问

运行 `docker compose ps` 检查 `rpa-frontend`，并确认 `FRONTEND_PORT` 没有被其他程序占用。

### 后端接口请求失败

运行 `docker compose logs -f rpa-backend`，确认 MySQL 与 Redis 已启动，并检查数据库连接参数和 JWT 密钥。

### 页面没有数据

确认 `rpa_system` 数据库已经初始化。首次启动时可使用 JPA 的 `ddl-auto=update` 补齐表结构，再导入演示数据。

### 修改前端代码后页面未变化

```bash
docker compose build rpa-frontend
docker compose --profile full up -d --force-recreate rpa-frontend
```

### 生产环境安全要求

- 更换管理员、MySQL、Redis 和第三方服务密码。
- 使用至少 32 字节的随机 JWT 密钥。
- 不要提交 `.env`、真实凭据、日志、上传文件或数据库备份。
- 仅开放必要端口，并限制管理端来源。
- 定期备份数据库，设置日志保留周期，并按最小权限分配角色。

## 许可证与使用

请根据仓库现有许可证和组织规范使用本项目。部署到生产环境前，应完成安全审计、压力测试、备份恢复验证及第三方服务配置检查。
