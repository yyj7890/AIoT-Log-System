# 发布前安全检查清单

适用范围：准备 `git add`、创建 GitHub 仓库、推送代码、发布截图或打包交付物之前。此清单只检查文件名、引用和脱敏状态；**不要在终端、聊天记录或文档中输出、复制或读取真实密码、Token 内容。**

## 凭证与配置

- [ ] 未提交真实 MQTT 用户名、密码、发现 Token、API Token、Cookie、私钥或其他密钥。
- [ ] 未提交 Mosquitto 密码文件（包括 `config/mosquitto-passwords`、`docker/local/mosquitto-passwords` 及其变体）。
- [ ] 真实凭证仅保存在本机受保护的配置中；仓库只提供不含真实值的示例文件，例如 `.env.example`、`application.example.yml` 和 `*.example.conf`。
- [ ] 文档、命令示例、截图和 Git 历史待提交内容均不包含真实凭证或可复用 Token。

## 设备、网络与运行数据

- [ ] 未提交真实设备 MAC、序列号、SSID、局域网/公网 IP、主机名或地理位置信息。
- [ ] 未提交真实设备运行日志、浏览器导出、抓包、数据库文件、数据库备份或生产数据。
- [ ] 截图只使用演示数据或已充分打码的数据；MQTT 用户名、密码、Token 和 Broker 地址必须完整遮挡。
- [ ] 原始截图、私有截图和抓包保留在 `screenshots/raw/`、`screenshots/private/` 或其他非版本控制位置。

## `.gitignore` 覆盖核对

- [ ] `.env`、`docker/local/`、本地环境文件、`backend/src/main/resources/application.yml`、`config/mqtt-credentials.env`、本地 Mosquitto 配置和 Mosquitto 密码文件已被忽略或未进入待提交列表。
- [ ] `mysql-data/`、运行日志目录、抓包、私有截图和常见数据库导出文件已被忽略。
- [ ] 新增本地配置或工具输出后，先确认其是否需要新增忽略规则；不要以“文件暂时未追踪”为由跳过检查。

## 提交前人工复核

- [ ] 检查 `git status --short`，确认没有意外的配置、数据、截图或二进制文件。
- [ ] 检查待提交文件名与 diff；如发现疑似凭证或真实运行数据，停止提交、移出仓库工作区或添加忽略规则后再处理。
- [ ] 验证 README、截图和架构图不夸大验证范围，并清晰写明可信局域网、无 TLS、全局凭证等限制。
- [ ] 未将 TCP `1883`、UDP `19830` 或数据库端口直接暴露到公网；公网发布前另行完成 TLS、每设备凭证、最小 ACL 与访问控制设计。
