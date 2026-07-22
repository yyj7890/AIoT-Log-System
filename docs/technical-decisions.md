# AIoT 智能设备运行日志管理系统 - 技术决策记录

## 0. 全局 MQTT 凭证模式（2026-07-13）

当前家庭/开发阶段已启用一套全局设备 MQTT 用户名密码并关闭匿名访问，所有设备通过自己的配网页填写该凭证后上传日志；Topic 和 `deviceCode` 用于业务识别。选择原因是接入新设备简单，且设备未填写凭证时不影响 AI 功能。限制是持有该凭证的设备可伪造其他设备 Topic，因此生产或多设备正式场景应升级为每设备凭证、ACL 和 TLS。

## 0.1 运行日志汇总与启动边界（2026-07-13）

最终选择：

- 设备仍按事件逐条发布 MQTT `/log` 消息。
- 后端按同一设备、30 秒窗口汇总为一条可读日志。
- `startup` 和 `firmware_started` 强制开始新的汇总批次。

原因：

- 设备一次启动会产生初始化、Wi-Fi、日志 MQTT、AI 协议等多条事件，逐条展示会淹没正常日志。
- 仅按时间窗口合并会在设备复位发生在 30 秒内时，把前一次启动末尾事件误并入下一次。
- 以启动事件作为边界，既保留设备原始事件，也能正确区分两次启动。

显示约定：

- 后端负责新日志中文化，前端兼容历史英文内容。
- `Local AI server discovered` 只表示“已发现本地 AI 服务”，不得推断为连接成功。
- `Local AI WebSocket hello completed` 表示“本地 AI WebSocket 握手完成”。

## 0.2 日志页面实时性与批量删除（2026-07-13）

最终选择：

- 日志管理页采用页面可见时每 5 秒 HTTP 轮询，而非为当前本地开发项目新增 WebSocket/SSE 推送。
- 批量删除默认隐藏勾选列，用户主动进入“批量删除”模式后才显示；删除必须二次确认。
- 后端批量删除先校验全部 ID 存在，再在同一事务中删除日志和标签关联。

原因：

- 日志上报频率低，5 秒轮询已能满足实机查看；实现和故障排查成本低，也不会额外影响 MQTT 链路。
- 默认隐藏勾选避免日常浏览界面拥挤和误触。
- Element Plus 跨页保留选择在删除后必须显式清空，否则会提交旧 ID；完成或取消时统一清空可避免“日志不存在”错误。

## 0.3 本地启动就绪检查（2026-07-13）

最终选择：`start-all.ps1` 保持“端口可用后再进入下一服务”的启动顺序，端口轮询间隔设为 200 毫秒。

原因：后端 Java 初始化通常约 2 秒，不能为了表面速度跳过就绪检查；将原 1 秒轮询缩短为 200 毫秒，可以减少已启动服务被脚本额外等待的时间，并避免网页过早打开。

## 0.4 GitHub 展示与发布材料边界（2026-07-13）

最终选择：以根目录 `README.md` 作为面向 GitHub 读者的项目入口；使用 `docs/architecture.mmd` 作为唯一可维护架构图源；使用 `screenshots/README.md` 管理截图清单和脱敏标准；使用 `SECURITY-CHECKLIST.md` 作为上传前人工复核清单。

原因：项目已完成实机验收，但真实设备、局域网和 MQTT 凭证信息不适合直接公开。将展示素材、设计事实和安全核对分开维护，既能让读者理解真实接入成果，也能避免 README 复制多份架构或将敏感信息混入示例。

边界：该仓库是独立 AIoT 系统；小智固件必须单独基于官方 `78/xiaozhi-esp32` Fork 发布。公开截图只能使用演示数据或已完成脱敏的数据，不能提交真实 MQTT 用户名、密码、Token、Mosquitto 密码文件、设备 MAC/IP、运行日志或数据库数据。

### 配置公开策略补充

公开仓库使用 `*.example.*` 作为可提交模板，本地运行文件保留真实路径和凭证但必须忽略。模板只展示键名、协议、端口、Topic 和占位符；不得保留真实账户、密码、Token、MAC、IP 或个人目录。由于 `.gitignore` 只影响未跟踪文件，任何已进入 Git 历史的本地配置必须在首次公开推送前通过干净历史另行处理。

## 0.5 Docker 私有配置引导（2026-07-13）

最终选择：Docker 首次部署不再从公开仓库的默认密码或匿名 MQTT 启动。`docker-update.cmd` 和 `docker-start.cmd` 先执行 `tools/initialize-docker-config.ps1`，在 Git 忽略的 `.env` 与 `docker/local/` 自动生成或复用数据库密码、全局 MQTT 凭证、Mosquitto 密码文件、ACL 和 Spring Boot MQTT 属性；Docker 仅挂载该私有目录，Mosquitto 固定关闭匿名访问。UDP 发现 Token 默认留空，只有设备固件也已预置同一 Token 时才由部署者手动启用。

原因：公开 Docker Compose 若携带 `root/root`、空密码或 `allow_anonymous true`，使用者一键启动后容易在不知情下获得不安全的 Broker，也会诱导把真实配置写回仓库。私有初始化同时保留一键部署体验和 GitHub 可公开性。脚本在每次启动时刷新用于 UDP `19830` 响应的宿主机局域网 IPv4，以适应 Wi-Fi、网线和手机热点切换。

边界：初始化文件只允许存放在本机，不得查看、复制、提交或上传。该方案仍是可信局域网方案，不能替代 TLS、每设备凭证、最小 ACL、管理登录或公网访问控制。更新 MQTT 凭证后需要重启 Docker 相关容器使 Broker 与后端重新加载配置。

## 0.6 GHCR 成品镜像发布（2026-07-13）

最终选择：源码构建版继续保留给开发者；同时使用 GitHub Actions 构建前端、后端成品镜像并推送 GitHub Container Registry。使用者通过 `docker-compose.ghcr.yml` 和 `docker-ghcr-*.cmd` 拉取镜像，不在本机运行 Maven 或 npm 构建。

原因：Docker Compose 本地构建虽不要求预装 Java/Node，但第一次下载依赖、构建镜像耗时长，也更容易受网络和环境差异影响。成品镜像让部署者只需 Docker Desktop，并仍由本机生成私有数据库/MQTT 配置，兼顾易用性与凭证隔离。

边界：GitHub Actions 使用工作流 `GITHUB_TOKEN` 写入包，不把个人 Token 写入仓库。首次发布后，维护者必须在 GitHub Packages 手动将两个镜像设为 Public；镜像公开不代表 MQTT 可暴露公网，运行服务仍限可信局域网。`latest` 供体验使用，演示和生产复现应固定 `AIOT_IMAGE_TAG` 到版本标签或提交 SHA。

## 0.7 Flyway 无损数据库升级（2026-07-22）

最终选择：后端接入 Flyway，并把 `backend/src/main/resources/db/migration/V1__create_initial_schema.sql` 作为数据库版本 1。空数据库正常执行 V1；已经存在完整业务表但没有 Flyway 历史表的数据库使用 `baseline-on-migrate=true`、`baseline-version=1` 建立基线，不重建表和不删除数据。以后每次结构变化只新增不可变的 V2、V3 迁移。

原因：Docker 命名卷只能保证数据库文件在更换镜像时仍存在，不能自动处理新镜像所需的字段、索引或表结构。Flyway让数据库结构随后的端镜像版本演进，升级不再依赖删除数据卷或手工重建数据库。

兼容边界：`sql/schema.sql` 冻结为 V1 兼容引导，暂时保留给已经发布的 pre-Flyway 镜像和 MySQL 空卷初始化，后续不得继续修改它；所有 V2 及以后变化必须进入 Flyway。首次把 Flyway 镜像部署到旧数据卷前仍应备份数据库，并继续复用同一命名卷和私有配置，禁止使用 `down -v`。

## 1. 后端 Java 版本

最终选择：

- JDK 17

原因：

- 本机已安装 JDK 17，路径为 `C:\Program Files\Java\jdk-17`。
- Spring Boot 3 要求 Java 17 或更高版本。
- JDK 17 是长期支持版本，适合课程项目、简历项目和后续扩展。

本机检查结果：

```text
java version "17.0.12" 2024-07-16 LTS
javac 17.0.12
```

注意：

- 当前系统默认 `java` 命令仍指向 Java 8。
- 如需运行 Spring Boot 3 后端，需要将 `JAVA_HOME` 指向 JDK 17。

PowerShell 临时切换方式：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

## 2. 后端 Spring Boot 版本

最终选择：

- Spring Boot 3.3.5

原因：

- Spring Boot 3 是当前更现代的主流版本。
- 项目刚开始，切换成本低。
- 配合 JDK 17 更适合长期维护和简历展示。

影响：

- 校验相关包使用 `jakarta.validation.*`。
- 不再使用 Spring Boot 2 中常见的 `javax.validation.*`。

示例：

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
```

## 3. 后端 ORM 方案

最终选择：

- MyBatis Plus
- `mybatis-plus-spring-boot3-starter`

原因：

- 适合 Spring Boot + MySQL 项目。
- 能减少基础 CRUD 代码。
- 国内课程、实习和企业项目中比较常见。

## 4. 数据库

最终选择：

- MySQL 8.x

原因：

- 与项目规划一致。
- 适合设备、日志、标签这类结构化业务数据。
- 与 Spring Boot、MyBatis Plus 配合成熟。

数据库名：

```text
aiot_log_system
```

## 5. 后端项目结构

当前结构：

```text
backend
├── src/main/java/com/aiot/log
│   ├── common
│   ├── config
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── exception
│   ├── mapper
│   ├── service
│   └── vo
└── src/main/resources
```

说明：

- `controller`：REST API 入口。
- `service`：业务逻辑。
- `mapper`：数据库访问。
- `entity`：数据库实体。
- `dto`：请求参数对象。
- `vo`：响应对象。
- `common`：统一响应、分页结果等公共结构。
- `exception`：业务异常和全局异常处理。
- `config`：框架配置。

## 6. Maven 状态

当前状态：

- 已在项目目录安装 Maven 3.9.16。

安装位置：

```text
D:\AI\IOT\tools\apache-maven-3.9.16
```

验证结果：

```text
Apache Maven 3.9.16
Maven home: D:\AI\IOT\tools\apache-maven-3.9.16
Java version: 17.0.12
```

说明：

- Maven 没有安装到系统全局目录。
- 当前采用项目本地 Maven，避免修改系统环境变量。
- 每次命令行运行后端时，需要临时把 JDK 17 和 Maven 加入当前 PowerShell 会话的 `Path`。

推荐：

- 后续如果想全局使用 `mvn`，可以再配置系统环境变量。

运行命令：

```powershell
cd D:\AI\IOT\backend
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;D:\AI\IOT\tools\apache-maven-3.9.16\bin;$env:Path"
mvn spring-boot:run
```

## 7. 当前已实现范围

已完成：

- Spring Boot 3 后端基础项目
- MySQL 连接配置
- MyBatis Plus 配置
- 跨域配置
- 统一 API 响应
- 全局异常处理
- 设备管理接口
- 后端首次编译验证通过

已实现设备接口：

```text
GET    /api/devices
GET    /api/devices/{id}
POST   /api/devices
PUT    /api/devices/{id}
DELETE /api/devices/{id}
```

未完成：

- 日志管理接口
- 标签管理接口
- 首页统计接口
- 枚举接口
- 前端项目

## 8. 编译验证记录

2026-07-06 首次后端编译通过。

编译命令：

```powershell
cd D:\AI\IOT\backend
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;D:\AI\IOT\tools\apache-maven-3.9.16\bin;$env:Path"
mvn -s D:\AI\IOT\tools\maven-settings.xml clean compile
```

结果：

```text
BUILD SUCCESS
Compiling 19 source files with javac [debug parameters release 17]
```

详细开发过程、报错和修复记录见：

```text
docs/development-log.md
```

早期后端实现顺序已完成，历史原文保存在：

```text
history/docs-before-consolidation-2026-07-09.zip
```

## 9. 2026-07-07 本地运行决策补充

### 9.1 MySQL 本地运行方式

当前采用本机 MySQL Server 8.4.9。

安装目录：

```text
C:\Program Files\MySQL\MySQL Server 8.4
```

项目本地数据目录：

```text
D:\AI\IOT\mysql-data
```

原因：

- 数据库文件放在项目目录下，便于记录和迁移当前实验环境。
- 不依赖 Windows 服务注册，适合开发阶段快速启动和停止。

当前启动命令：

```powershell
Start-Process -FilePath 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' -ArgumentList '--basedir="C:\Program Files\MySQL\MySQL Server 8.4" --datadir="D:\AI\IOT\mysql-data" --port=3306 --bind-address=127.0.0.1' -WindowStyle Hidden
```

### 9.2 JDBC URL 参数

当前 Spring Boot 数据库连接 URL：

```text
jdbc:mysql://localhost:3306/aiot_log_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

新增 `allowPublicKeyRetrieval=true` 的原因：

- MySQL 8 默认认证方式使用 `caching_sha2_password`。
- 本地开发环境使用 root/root 连接时，JDBC 可能报错 `Public Key Retrieval is not allowed`。
- 加上该参数后，数据库接口可以正常连接。

### 9.3 后端启动方式

当前真实联调优先使用：

```powershell
mvn -s D:\AI\IOT\tools\maven-settings.xml spring-boot:run
```

说明：

- `spring-boot:run` 是长驻命令，启动成功后不会自动退出。
- 在工具环境中应使用后台进程启动，并通过日志和端口判断状态。
- `mvn package` 当前在 Windows 上遇到 jar 重命名失败，暂不作为主要启动路径。

### 9.4 当前后端接口状态

截至 2026-07-07，以下接口已完成真实 MySQL 联调：

```text
GET  /api/enums
GET  /api/dashboard/summary
GET  /api/logs?page=1&pageSize=3
GET  /api/tags
POST /api/tags
```

结果均为：

```text
HTTP 200 / code 200
```

## 10. 2026-07-07 前端技术决策

前端采用：

```text
Vue 3 + TypeScript + Vite
Vue Router
Pinia
Element Plus
Axios
@element-plus/icons-vue
```

原因：

- 与项目文档中的前端设计一致。
- Vue 3 + Vite 适合快速搭建管理系统。
- Element Plus 提供表格、表单、弹窗、标签、分页等后台管理常用组件。
- Axios 统一封装后端 API 调用。
- Pinia 当前用于维护枚举缓存，后续可以扩展用户、权限或全局配置状态。

本地开发端口：

```text
5173
```

后端代理：

```text
/api -> http://127.0.0.1:8080
```

当前构建命令：

```powershell
cd D:\AI\IOT\frontend
npm run build
```

验证结果：

```text
BUILD SUCCESS
```

## 11. 2026-07-07 一键启动方式

当前采用根目录脚本统一启动开发环境：

```text
start-all.ps1
start-all.cmd
```

选择原因：

- 开发阶段需要同时启动 MySQL、后端和前端。
- 手动分别输入三组命令容易出错。
- 一键脚本可以先检查端口，避免重复启动服务。
- 当前不改变前后端项目结构，适合开发联调阶段。

脚本检查端口：

```text
MySQL   3306
Backend 8080
Frontend 5173
```

脚本最终打开：

```text
http://127.0.0.1:5173/
```

后续如果进入部署阶段，可以再改成：

```text
前端 build 后放入后端静态资源，只启动一个 Spring Boot 服务。
```

## 12. 2026-07-08 当前技术栈展示判断

当前技术栈：

```text
前端：Vue 3 + TypeScript + Vite + Element Plus
后端：Spring Boot 3.3.5 + MyBatis Plus
数据库：MySQL 8.4
设备上报：HTTP + MQTT
MQTT Broker：Mosquitto
```

判断：

- 该技术栈不落后，适合作为 Java 后端、全栈或 IoT 管理系统作品展示。
- Spring Boot 3 + Java 17 是现代 Java 后端组合。
- Vue 3 + TypeScript + Element Plus 适合后台管理系统。
- MySQL、MyBatis Plus、Docker、Redis、Nginx、JWT 等是国内 Java 岗位常见组合。
- MQTT / Mosquitto 能体现 IoT 场景，不是普通 CRUD 项目的重复堆叠。

后续补强项不在本文档展开，统一归档到：

```text
docs/future-development-backlog.md
```

## 13. 2026-07-09 Docker Compose 部署架构

决定：

- 使用 Docker Compose 编排前端、后端、MySQL 和 Mosquitto。
- 前端采用多阶段构建，最终由 Nginx 提供静态页面并代理 `/api`。
- 后端采用 Maven 构建镜像和 JRE 运行镜像分离的多阶段构建。
- MySQL 与 Mosquitto 数据使用 Docker 命名卷持久化。

原因：

- 用户只需安装 Docker，不需要分别配置 Java、Node.js、MySQL 和 Mosquitto。
- 开发环境与部署环境隔离，减少机器差异。
- Nginx 统一提供网页和 API 入口，符合常见部署方式。
- 命名卷允许容器重建后继续保留数据。

## 14. 2026-07-09 Docker 启动与更新脚本分离

决定：

```text
docker-start.cmd   只启动已有容器和镜像
docker-update.cmd  代码更新后重新构建并启动
docker-stop.cmd    停止容器但保留数据
```

原因：

- 日常启动不需要重复执行 Maven 和 npm 构建。
- 首次构建耗时较长，启动与更新混在一个脚本中会影响使用体验。
- 启动脚本必须等待后端 API 和前端都可用后再打开浏览器，避免页面初始请求返回 `502`。

## 15. 2026-07-09 Docker 构建网络策略

决定：

- 基础镜像仍优先使用官方镜像。
- 后端 Maven 构建使用 `backend/maven-settings-docker.xml` 国内公共镜像。
- Maven 下载增加重试，并使用 BuildKit 缓存本地依赖。
- 正式发布时构建前端、后端成品镜像，并评估国内容器镜像仓库。

原因：

- 当前网络访问 Docker Hub 和 Maven Central 存在 TLS 超时、下载中断和 OAuth 鉴权失败。
- 官方基础镜像来源明确，但首次拉取可能需要稳定代理。
- 国内 Maven 镜像和依赖缓存可以显著减少后端重复下载。
- 成品镜像可以避免使用者现场安装 Node、Maven 和 JDK。

## 16. 2026-07-09 中文编码策略

决定：

- 源码、Markdown 和 SQL 文件统一保存为 UTF-8。
- MySQL 数据库和表使用 `utf8mb4`。
- Flyway V1 和兼容初始化 SQL 使用 `utf8mb4`，JDBC 固定使用 UTF-8。
- JDBC 使用 Java `UTF-8` 字符集。
- Spring Boot Servlet 响应强制 UTF-8。

原因：

- Docker 首次初始化时曾出现中文被按单字节编码解释后存入数据库的问题。
- 仅设置数据库表字符集不足以保证 SQL 客户端、JDBC 和 HTTP 响应链路一致。

## 17. 2026-07-09 交付方式选择

当前决定：

- 开发者本地部署使用 Docker Compose。
- 普通用户后续优先使用在线网页。
- 桌面安装包暂不作为当前开发目标。

原因：

- 桌面应用不仅要包装 Vue 页面，还要管理 Java 后端、数据库、Mosquitto、端口和数据目录，改造成本较高。
- Docker Compose 更适合当前 Java + MySQL + MQTT 的多服务架构。
- 在线部署对普通用户最简单，不需要安装 Docker 或其他依赖。

## 18. 2026-07-09 文档维护结构

决定：

- `PROJECT_CONTEXT.md` 作为新对话的项目交接入口。
- `docs` 只维护项目设计、当前状态、开发记录、后续计划、技术决策和文档导航。
- 合并前的原始文档统一保存在 `history/`。
- 不再为每个小功能创建新的 Markdown。

原因：

- 原文档存在状态、计划和开发记录重复，查找成本较高。
- 固定文档职责后，新对话和人工查阅都能快速定位信息。
- 历史原文仍然保留，不因精简当前文档而丢失。

## 19. 技术选型对比与边界

### MyBatis Plus 与 JPA

选择 MyBatis Plus：

- 国内 Java 项目常见，CRUD 开发直接。
- SQL 行为更容易观察，适合学习数据库和后台管理系统。
- 当前业务查询复杂度不高。

没有选择 JPA 的原因：

- JPA 更强调对象关系映射和实体关系管理，当前项目收益有限。
- 学习成本和隐式行为更多。

边界：如果后续领域关系复杂、需要丰富的实体生命周期管理，可以重新评估 JPA；如果 SQL 越来越复杂，应补充 XML 或自定义 SQL，而不是强行依赖通用 CRUD。

### MySQL 与 SQLite

选择 MySQL：

- 更接近企业 Java 后端岗位和多用户服务部署。
- 适合持续增长的设备、日志和上报数据。
- Docker 和服务器部署成熟。

没有选择 SQLite 的原因：

- SQLite 更适合单机嵌入式应用，不适合作为当前网络服务的主要数据库。

边界：如果未来制作完全离线的单机桌面版，SQLite 会比捆绑 MySQL 更合适。

### HTTP 与 MQTT

同时保留：

- HTTP 调试简单，设备可以直接请求后端。
- MQTT 消息开销小，支持发布订阅、QoS 和断线重连，更适合持续上报。

边界：HTTP 与 MQTT 不是必须同时使用。真实设备确定后可选择主协议，但后端应继续复用统一上报服务。

### Mosquitto 与大型消息平台

选择 Mosquitto：

- 轻量、标准 MQTT、安装和 Docker 部署简单。
- 足够支持当前单机和局域网项目。

边界：大规模设备集群、规则引擎和高可用场景可评估 EMQX 等平台，当前没有必要增加复杂度。

### Docker Compose 与桌面安装包

选择 Docker Compose：

- 当前系统由四个服务组成，容器编排能保持结构清晰。
- 更符合服务器部署和工程展示。

边界：Docker 面向开发者和部署人员，不是普通用户零安装方案。普通用户最终更适合在线网页；真正离线桌面版需要重新设计数据库和服务生命周期。
