# Springboot-RPA

RPA 运营管理系统，提供机器人、流程、任务、调度、数据采集、报表和通知等自动化运营能力。项目采用 Spring Boot 后端与 Vue 3 前端分离开发，适合本地开发、服务器部署和 Docker 部署。

## 功能概览

- 用户登录、JWT 鉴权、权限控制和安全审计
- 机器人、流程、任务、队列、触发器和执行日志管理
- 可视化流程设计器、任务监控和运行状态看板
- 数据采集、数据处理、报表分析和水印管理
- 邮件、钉钉、企业微信、飞书等通知配置
- WebSocket 实时消息、Quartz 定时任务、Redis 缓存支持
- AI 助手和代码生成相关接口配置

## 技术栈

后端：

- Java 17
- Spring Boot 2.7.18
- Spring Security / JWT
- Spring Data JPA
- MySQL 8
- Redis
- Quartz
- WebSocket

前端：

- Vue 3
- Vite
- Element Plus
- ECharts
- Axios
- Vue Router

## 目录结构

```text
.
├── src/main/java/rpa              # 后端源码
├── src/main/resources             # 后端配置、SQL 初始化脚本、静态资源
├── src/test/java/rpa              # 后端测试
├── src/views                      # 前端页面
├── src/components                 # 前端组件
├── src/api                        # 前端接口封装
├── src/router                     # 前端路由
├── public                         # 前端公共资源
├── pom.xml                        # Maven 配置
├── package.json                   # 前端依赖和脚本
├── vite.config.js                 # Vite 配置
├── docker-compose.yml             # Docker Compose 示例
├── Dockerfile                     # 后端镜像构建文件
├── Dockerfile.frontend            # 前端镜像构建文件
└── README.md
```

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+
- MySQL 8.x
- Redis 6.x，可按需启用

本地默认数据库配置：

```text
DB_HOST=localhost
DB_PORT=3307
DB_NAME=rpa_system
DB_USERNAME=root
DB_PASSWORD=123456
```

生产环境请通过环境变量覆盖默认密码、JWT 密钥、数据库地址、Redis 地址和第三方通知密钥，不要提交 `.env` 文件。

## 快速启动

1. 克隆项目

```bash
git clone https://github.com/lwxiaoye/Springboot-RPA.git
cd Springboot-RPA
```

2. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS rpa_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 初始化数据

按需导入 `src/main/resources/init-db.sql` 以及同目录下的增量 SQL。开发环境也可以使用 JPA 的 `ddl-auto=update` 自动补齐表结构。

4. 启动后端

```bash
mvn spring-boot:run
```

后端默认地址为 `http://localhost:8080`。

5. 启动前端

```bash
npm install
npm run dev
```

前端默认地址为 `http://localhost:5173`。开发代理会把 `/api` 请求转发到后端服务。

6. 登录系统

初始化数据或本地开发库可使用：

```text
账号：admin
密码：123456
```

如果数据库中已修改账号或密码，请以实际数据为准。

## Docker 部署

复制环境变量示例并按服务器配置修改：

```bash
cp .env.example .env
docker compose up -d --build
```

`.env` 中应配置数据库、Redis、JWT、跨域、邮件和通知相关参数。`.env` 已被 Git 忽略，不应上传到仓库。

## 常用配置

主要配置文件：

- `src/main/resources/application.yml`：本地开发默认配置
- `src/main/resources/application-prod.yml`：生产环境配置示例
- `.env.example`：Docker / 服务器环境变量示例

常用环境变量：

```text
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=3307
DB_NAME=rpa_system
DB_USERNAME=root
DB_PASSWORD=123456
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=please-change-this-secret-to-at-least-32-bytes
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

## 构建与测试

后端测试：

```bash
mvn test
```

前端构建：

```bash
npm run build
```

## 仓库文件说明

仓库只保留项目运行、构建和部署所需文件。以下内容不会上传：

- `target/`、`dist/`、`node_modules/` 等构建产物
- `.env`、日志、上传文件和本地缓存
- IDE 配置、临时脚本、本地部署草稿和历史说明文档

