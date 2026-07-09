# 开发记录

本文件记录关键里程碑和可复用的故障处理方法，按日期倒序排列。

完整早期过程保存在 `../history/docs-before-consolidation-2026-07-09.zip`，包括原始命令、长篇报错和逐步搭建过程。

## 2026-07-09

### 文档结构整理

- 事项：原文档数量多，状态、计划和开发记录存在重复。
- 处理：新增根目录 `AGENTS.md`、`PROJECT_CONTEXT.md`；当前文档收敛为 6 份，合并前原文统一压缩保存到 `history/`。
- 结果：新对话只需先读取 `PROJECT_CONTEXT.md`，专题资料按 `docs/README.md` 查找。

### Docker 启动页面出现 502

- 问题：Nginx 已启动，但 Spring Boot 尚未就绪，浏览器过早打开导致 `/api` 返回 `502`。
- 处理：`docker-start.cmd` 先轮询后端统计接口，再检查前端，最后打开浏览器。
- 结果：启动页面不再因后端未就绪产生初始 502。

### Docker 初始化中文乱码

- 问题：页面固定文字正常，但 Docker 初始设备、标签和日志数据乱码。
- 根因：SQL 中文在首次导入时按错误客户端编码解释后存入。
- 处理：
  - 使用可逆字符集转换原地修复当前数据。
  - `schema.sql`、`init-data.sql` 增加 `SET NAMES utf8mb4`。
  - JDBC 显式使用 UTF-8，Servlet 响应强制 UTF-8。
- 验证：MySQL 查询和 HTTP 原始 UTF-8 响应均显示正确中文。

### Docker Compose 完整验收

- 服务：frontend、backend、mysql、mosquitto。
- 验证：MySQL 为 `healthy`；网页与后端接口返回正常；MQTT 状态为已连接。
- 脚本：
  - `docker-start.cmd`：日常快速启动。
  - `docker-update.cmd`：首次部署或代码更新后重建。
  - `docker-stop.cmd`：停止并保留数据。

### Docker Maven 依赖下载中断

- 问题：容器内访问 Maven Central 时出现 `Premature end of Content-Length`。
- 根因：国外仓库网络传输中断。
- 处理：
  - 新增 `backend/maven-settings-docker.xml`，使用国内 Maven 公共镜像。
  - Dockerfile 增加 Maven 下载重试。
  - 使用 BuildKit 缓存 `/root/.m2/repository`。
- 结果：后端镜像构建成功，重试时可复用已下载依赖。

### Docker Hub 镜像下载不稳定

- 问题：出现 TLS 握手超时、`unexpected EOF`、OAuth token 获取失败。
- 根因：Docker Desktop、WSL 2 与 VPN 代理路径未统一，且 Docker Hub 网络不稳定。
- 处理：
  - Docker Desktop 配置 HTTP/HTTPS 代理。
  - 网络不稳定时逐个拉取基础镜像。
  - 完成后依靠本地镜像缓存。
- 结果：MySQL、Mosquitto、Node、Nginx、Maven、JRE 镜像齐全并完成构建。
- 后续：正式发布成品镜像，并评估国内容器镜像仓库。

### Docker Compose 一键部署

- 新增前后端多阶段 Dockerfile。
- 新增 MySQL、Mosquitto、后端和前端 Compose 编排。
- Nginx 提供前端并反向代理 `/api`。
- 后端数据库和 MQTT 地址支持环境变量覆盖，继续兼容本地开发。

## 2026-07-08

### MQTT 正式化

- 后端使用 Eclipse Paho 订阅 `aiot/device/+/report`。
- MQTT 消息复用 HTTP 设备上报业务逻辑。
- 新增 MQTT 状态接口和前端状态页面。
- 增加消息参数校验、接收统计、失败统计和最近错误。
- 模拟脚本改用 UTF-8 临时文件发布 JSON，避免 PowerShell 参数传递丢失双引号。
- Mosquitto 本机真实发布/订阅联调通过。

### 第二阶段设备自动上报

- 完成 HTTP 上报接口和 `device_reports` 表。
- 异常上报自动生成来源为 `DEVICE` 的日志。
- 日志列表支持来源筛选和中文来源展示。
- 设备详情展示最近上报与温度、湿度、电压、信号趋势。
- 完成告警规则新增、编辑、删除和启停。
- 告警判断优先使用配置规则，没有规则时使用默认阈值。

### 前端交互调整

- 日志筛选修改后自动刷新。
- 调整时间筛选栏宽度，避免超出页面。
- 删除与自动筛选重复的刷新操作。
- 左侧菜单固定，右侧内容独立滚动。

## 2026-07-07

### 第一阶段功能验收

- 设备、日志、标签 CRUD 验证通过。
- 日志筛选、状态修改和首页统计验证通过。
- Vue 3 前端与 Spring Boot/MySQL 完成真实数据联调。

### 本地一键启动与停止

- 新增 `start-all.cmd/.ps1` 和 `stop-all.cmd/.ps1`。
- 启动脚本管理 MySQL、后端、前端和 Mosquitto。
- 修复浏览器打开时机、桌面快捷脚本工作目录和停止后页面仍保留等使用问题。

### 前端工程

- 创建 Vue 3 + TypeScript + Vite 项目。
- 集成 Element Plus、Pinia、Vue Router、Axios。
- 建立 Dashboard、设备、日志和标签页面。
- `/api` 由 Vite 代理到 Spring Boot `8080`。

### 后端与数据库联调

- 使用 Java 17、Spring Boot 3.3.5、MyBatis Plus。
- 完成设备、日志、标签、统计和枚举接口。
- MySQL 使用项目本地数据目录和初始化 SQL。
- 处理 JDBC 连接参数、Maven 本地仓库和 Windows JAR 占用问题。

## 2026-07-06

### 项目设计与后端基础

- 明确第一阶段需求、页面、枚举和验收标准。
- 完成数据库表、索引和关系设计。
- 完成 REST API 与前端页面结构设计。
- 创建 Spring Boot 基础工程、统一响应、异常处理和 MyBatis Plus 配置。

## 维护规则

- 新记录放在最上方对应日期下。
- 不使用跨日期连续序号。
- 每个故障使用“问题、根因、处理、验证”结构。
- 当前状态写入 `project-status.md`，未来任务写入 `future-development-backlog.md`。
- 只有需要追溯完整原始过程时才解压 `history/docs-before-consolidation-2026-07-09.zip`。

## 通用排错方法

按数据链路从外到内检查，不要同时修改多个位置：

1. 页面是否能打开：检查前端/Nginx。
2. `/api` 是否返回：检查代理和 Spring Boot。
3. 后端是否报连接错误：检查 MySQL、MQTT 和环境变量。
4. 数据是否入库：直接查询 MySQL。
5. 数据库正确但页面错误：检查 API 响应、前端字段和编码。
6. 仅 Docker 失败：比较容器地址与 `127.0.0.1`，容器之间应使用服务名。

常用命令：

```powershell
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
docker compose logs -f mosquitto
```

问题类型判断：

| 现象 | 优先检查 |
| --- | --- |
| 页面无法访问 | 前端进程、Nginx、端口 |
| 页面出现 502 | 后端未就绪、Nginx 代理 |
| API 500 | 后端日志、数据库 |
| MQTT 未连接 | Broker、地址、端口、订阅配置 |
| 镜像拉取失败 | Docker Hub 网络、代理 |
| 页面中文乱码 | 数据库存储、HTTP 原始字节、响应编码 |

学习总结：先确认问题属于哪一层，再查看该层日志；能够稳定复现后再修改，修改后用同一路径重新验证。
