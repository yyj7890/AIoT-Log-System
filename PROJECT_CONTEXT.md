# AIoT 项目上下文

更新时间：2026-07-09

这是新对话继续项目时的首要阅读文件。

## 1. 项目目标

AIoT 智能设备运行日志管理系统，用于管理设备、运行日志、设备上报数据、告警规则，并通过 HTTP/MQTT 接收真实设备数据。

## 2. 技术栈

- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios
- 后端：Java 17、Spring Boot 3.3.5、MyBatis Plus
- 数据库：MySQL 8.4
- MQTT：Mosquitto 2、Eclipse Paho
- 部署：Docker Compose、Nginx

## 3. 已完成功能

- 设备、日志、标签管理
- 日志状态、级别、类型、来源筛选
- 首页统计和设备详情
- HTTP 设备上报：`POST /api/device-reports`
- MQTT 订阅：`aiot/device/+/report`
- 正常/异常模拟上报脚本
- 异常上报自动生成来源为 `DEVICE` 的日志
- 最近上报数据和温度、湿度、电压、信号趋势
- 告警规则管理
- MQTT 状态接口和页面
- 左侧菜单固定、内容区独立滚动
- 本地一键启动/停止
- Docker Compose 四服务部署

## 4. 当前状态

Docker 联调已通过：

- frontend、backend、mysql、mosquitto 四容器正常运行
- MySQL 健康检查通过
- 网页与后端接口返回正常
- MQTT 后端连接正常
- Docker 初始化数据中文乱码已修复
- Docker 启动脚本会等待后端和前端就绪后再打开浏览器

真实硬件尚未接入，当前 MQTT/HTTP 上报主要使用模拟脚本验证。

项目文档已经整理：新对话读取本文件；当前进度、专题设计和历史归档由 `docs/README.md` 分类导航。

## 5. 启动方式

本地开发版：

```text
start-all.cmd
stop-all.cmd
网页：http://127.0.0.1:5173/
```

Docker 版：

```text
docker-start.cmd   日常快速启动
docker-update.cmd  首次部署或代码更新后重新构建
docker-stop.cmd    停止并保留数据
网页：http://127.0.0.1/
```

本地版和 Docker 版会争用 `3306`、`8080`、`1883`，不要同时启动。

## 6. 关键路径

- 后端：`backend/`
- 前端：`frontend/`
- SQL：`sql/`
- 模拟脚本：`tools/`
- Docker 编排：`docker-compose.yml`
- 常用文档：`docs/README.md`
- 文档导航：`docs/README.md`
- 项目设计：`docs/project-design.md`
- 当前进度：`docs/project-status.md`
- 故障与修复：`docs/development-log.md`

## 7. 下一步

优先顺序：

1. 等待真实硬件完成后进行局域网 HTTP/MQTT 联调。
2. 增加设备身份认证、MQTT 用户密码和接口安全。
3. 整理 GitHub 截图与项目展示。
4. 发布前后端成品镜像，并考虑同步到国内容器镜像仓库。
5. 后续再考虑登录权限、OpenAPI、测试、在线演示和 AI 分析。

## 8. 已知注意事项

- Docker Hub 在当前网络下可能需要 VPN/代理；基础镜像已在本机缓存。
- Docker 构建 Maven 依赖使用 `backend/maven-settings-docker.xml` 国内镜像和 BuildKit 缓存。
- 当前 Mosquitto 为可信局域网联调允许匿名访问，禁止直接暴露公网。
- SQL 初始化脚本必须保持 UTF-8，并显式执行 `SET NAMES utf8mb4`。
- Docker 数据保存在命名卷中，`docker compose down` 不删除数据；`down -v` 会删除数据。
